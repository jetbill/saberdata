package com.saberdata.mineria;

import com.saberdata.config.MineriaProperties;
import com.saberdata.dto.ArbolResponse;
import com.saberdata.dto.AtributoResponse;
import com.saberdata.dto.ModeloResponse;
import com.saberdata.dto.PrediccionResponse;
import com.saberdata.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minería de datos con Weka: entrena un árbol de decisión J48 que clasifica el rendimiento (ALTO / BAJO) a partir de
 * los atributos socioeconómicos y del colegio, lo evalúa con validación cruzada, lo guarda en disco y predice.
 *
 * <p>El entrenamiento es pesado, así que solo lo dispara un administrador. La predicción solo lee el modelo en memoria
 * (que se reemplaza entero al reentrenar), por lo que puede atender varias peticiones a la vez.
 */
@Service
public class MineriaService {

    private static final Logger log = LoggerFactory.getLogger(MineriaService.class);

    static final String ALGORITMO = "J48 (árbol de decisión C4.5) de Weka 3.8";
    private static final int MINIMO_FILAS = 100;
    private static final int MAX_LINEAS_ARBOL = 400;
    private static final long SEMILLA = 1L;

    private final JdbcTemplate jdbc;
    private final MineriaProperties props;
    private final AtomicBoolean entrenando = new AtomicBoolean();
    private volatile ModeloEntrenado modelo;

    public MineriaService(JdbcTemplate jdbc, MineriaProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    /** Si ya hay un modelo guardado de una ejecución anterior, lo carga al arrancar. */
    @PostConstruct
    void cargarModeloGuardado() {
        Path ruta = Path.of(props.modeloRuta());
        if (!Files.exists(ruta)) {
            log.info("Aún no hay modelo de minería de datos en {}. Un administrador puede entrenarlo.", ruta);
            return;
        }
        try {
            modelo = leer(ruta);
            log.info("Modelo de minería de datos cargado desde {} (entrenado el {}).", ruta, modelo.entrenadoEn());
        } catch (Exception ex) {
            log.warn("No se pudo leer el modelo guardado en {}: {}. Hay que reentrenarlo.", ruta, ex.toString());
        }
    }

    // ------------------------------------------------------------------ consultas

    public ModeloResponse estado() {
        ModeloEntrenado m = modelo;
        return m == null ? ModeloResponse.noDisponible() : respuesta(m);
    }

    public List<AtributoResponse> atributos() {
        Instances cabecera = modeloActual().cabecera();
        List<AtributoResponse> lista = new ArrayList<>();
        for (CatalogoAtributos.Atributo a : CatalogoAtributos.TODOS) {
            Attribute atributo = cabecera.attribute(a.clave());
            List<String> valores = new ArrayList<>();
            for (int i = 0; i < atributo.numValues(); i++) {
                valores.add(atributo.value(i));
            }
            lista.add(new AtributoResponse(a.clave(), a.etiqueta(), valores));
        }
        return lista;
    }

    public ArbolResponse arbol() {
        String[] lineas = modeloActual().arbol().split("\\R");
        boolean truncado = lineas.length > MAX_LINEAS_ARBOL;
        int n = Math.min(lineas.length, MAX_LINEAS_ARBOL);
        return new ArbolResponse(String.join("\n", java.util.Arrays.copyOf(lineas, n)), lineas.length, truncado);
    }

    // ------------------------------------------------------------------ predicción

    public PrediccionResponse predecir(Map<String, String> atributos) {
        ModeloEntrenado m = modeloActual();
        Instances cabecera = m.cabecera();

        for (String clave : atributos.keySet()) {
            if (CatalogoAtributos.porClave(clave).isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Atributo desconocido: " + clave + ".");
            }
        }

        // Una instancia nueva tiene todos los valores en "faltante"; solo se rellenan los que llegan y se conocen.
        DenseInstance instancia = new DenseInstance(cabecera.numAttributes());
        instancia.setDataset(cabecera);
        List<String> ignorados = new ArrayList<>();
        for (Map.Entry<String, String> e : atributos.entrySet()) {
            String valor = e.getValue();
            if (valor == null || valor.isBlank()) {
                continue;
            }
            Attribute atributo = cabecera.attribute(e.getKey());
            int indice = atributo.indexOfValue(valor.trim());
            if (indice < 0) {
                ignorados.add(e.getKey());   // categoría que el modelo no vio al entrenar
            } else {
                instancia.setValue(atributo, indice);
            }
        }

        try {
            double[] distribucion = m.clasificador().distributionForInstance(instancia);
            int alto = cabecera.classAttribute().indexOfValue("ALTO");
            int bajo = cabecera.classAttribute().indexOfValue("BAJO");
            double pAlto = distribucion[alto];
            double pBajo = distribucion[bajo];
            return new PrediccionResponse(pAlto >= pBajo ? "ALTO" : "BAJO", pAlto, pBajo, ignorados);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo calcular la predicción.");
        }
    }

    // ------------------------------------------------------------------ entrenamiento

    /** Lee los datos de la base, entrena y evalúa el modelo, lo guarda en disco y lo deja activo. */
    public ModeloResponse entrenar() {
        if (!entrenando.compareAndSet(false, true)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya hay un entrenamiento en curso. Espera a que termine.");
        }
        try {
            List<String[]> filas = leerFilas();
            if (filas.size() < MINIMO_FILAS) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "No hay datos suficientes para entrenar (" + filas.size() + " filas). Importa antes el dataset.");
            }
            ModeloEntrenado nuevo = entrenarConFilas(filas);
            try {
                guardar(nuevo, Path.of(props.modeloRuta()));
            } catch (IOException ex) {
                log.warn("El modelo se entrenó pero no se pudo guardar en {}: {}", props.modeloRuta(), ex.toString());
            }
            modelo = nuevo;
            return respuesta(nuevo);
        } finally {
            entrenando.set(false);
        }
    }

    /**
     * Núcleo del entrenamiento, separado de la base de datos para poder probarlo con datos de ejemplo.
     * Cada fila trae los 17 atributos (en el orden de {@link CatalogoAtributos#TODOS}) y, al final, la clase.
     */
    ModeloEntrenado entrenarConFilas(List<String[]> filas) {
        long inicio = System.nanoTime();
        try {
            Instances datos = construirInstancias(filas);

            Evaluation evaluacion = new Evaluation(datos);
            evaluacion.crossValidateModel(nuevoJ48(), datos, props.pliegues(), new Random(SEMILLA));

            J48 clasificador = nuevoJ48();
            clasificador.buildClassifier(datos);

            int alto = datos.classAttribute().indexOfValue("ALTO");
            int bajo = datos.classAttribute().indexOfValue("BAJO");
            int[] conteos = datos.attributeStats(datos.classIndex()).nominalCounts;
            double base = (double) Math.max(conteos[alto], conteos[bajo]) / datos.numInstances();

            double[][] matriz = evaluacion.confusionMatrix();
            long[][] matrizEntera = new long[matriz.length][matriz.length];
            for (int i = 0; i < matriz.length; i++) {
                for (int j = 0; j < matriz.length; j++) {
                    matrizEntera[i][j] = Math.round(matriz[i][j]);
                }
            }

            Metricas metricas = new Metricas(
                    datos.numInstances(), datos.numAttributes() - 1, props.pliegues(), props.minObj(),
                    limpio(evaluacion.pctCorrect() / 100.0), limpio(base), limpio(evaluacion.kappa()),
                    limpio(evaluacion.areaUnderROC(alto)),
                    new Metricas.ClaseMetricas(limpio(evaluacion.precision(alto)), limpio(evaluacion.recall(alto)),
                            limpio(evaluacion.fMeasure(alto))),
                    new Metricas.ClaseMetricas(limpio(evaluacion.precision(bajo)), limpio(evaluacion.recall(bajo)),
                            limpio(evaluacion.fMeasure(bajo))),
                    matrizEntera,
                    (int) clasificador.measureNumLeaves(), (int) clasificador.measureTreeSize(),
                    (System.nanoTime() - inicio) / 1_000_000_000L);

            return new ModeloEntrenado(clasificador, new Instances(datos, 0), metricas, clasificador.toString(),
                    Instant.now());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Falló el entrenamiento del modelo", ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo entrenar el modelo: " + ex.getMessage());
        }
    }

    private J48 nuevoJ48() {
        J48 j48 = new J48();
        j48.setMinNumObj(props.minObj());
        return j48;
    }

    private static Instances construirInstancias(List<String[]> filas) {
        int nAtributos = CatalogoAtributos.TODOS.size();

        // Los atributos nominales de Weka necesitan declarar sus valores posibles de antemano.
        List<TreeSet<String>> valores = new ArrayList<>();
        for (int i = 0; i < nAtributos; i++) {
            valores.add(new TreeSet<>());
        }
        for (String[] fila : filas) {
            for (int i = 0; i < nAtributos; i++) {
                if (fila[i] != null) {
                    valores.get(i).add(fila[i]);
                }
            }
        }

        ArrayList<Attribute> atributos = new ArrayList<>();
        for (int i = 0; i < nAtributos; i++) {
            List<String> posibles = valores.get(i).isEmpty() ? List.of("desconocido") : new ArrayList<>(valores.get(i));
            atributos.add(new Attribute(CatalogoAtributos.TODOS.get(i).clave(), posibles));
        }
        atributos.add(new Attribute(CatalogoAtributos.CLASE, CatalogoAtributos.VALORES_CLASE));

        Instances datos = new Instances("saber11", atributos, filas.size());
        datos.setClassIndex(nAtributos);
        for (String[] fila : filas) {
            String clase = fila[nAtributos];
            if (!CatalogoAtributos.VALORES_CLASE.contains(clase)) {
                continue;
            }
            DenseInstance instancia = new DenseInstance(nAtributos + 1);
            instancia.setDataset(datos);
            for (int i = 0; i < nAtributos; i++) {
                if (fila[i] != null) {
                    instancia.setValue(i, datos.attribute(i).indexOfValue(fila[i]));
                }
            }
            instancia.setValue(nAtributos, datos.classAttribute().indexOfValue(clase));
            datos.add(instancia);
        }
        return datos;
    }

    private List<String[]> leerFilas() {
        String sql = "select " + CatalogoAtributos.columnasSql()
                + " from resultado_saber11 where " + CatalogoAtributos.COLUMNA_CLASE + " is not null";
        int columnas = CatalogoAtributos.TODOS.size() + 1;
        List<String[]> filas = new ArrayList<>(120_000);
        // Los valores se repiten miles de veces: se reutiliza la misma cadena para ahorrar memoria.
        Map<String, String> unicos = new HashMap<>();
        jdbc.query(sql, (RowCallbackHandler) rs -> {
            String[] fila = new String[columnas];
            for (int i = 0; i < columnas; i++) {
                String v = rs.getString(i + 1);
                fila[i] = v == null ? null : unicos.computeIfAbsent(v, k -> k);
            }
            filas.add(fila);
        });
        return filas;
    }

    // ------------------------------------------------------------------ persistencia

    /** Guarda el modelo en un archivo temporal y lo mueve al definitivo, para no dejar un archivo a medias. */
    void guardar(ModeloEntrenado m, Path ruta) throws IOException {
        Path padre = ruta.toAbsolutePath().getParent();
        if (padre != null) {
            Files.createDirectories(padre);
        }
        Path temporal = ruta.resolveSibling(ruta.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temporal))) {
            out.writeObject(m);
        }
        Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Lee un modelo guardado. Solo debe usarse con archivos que genera esta misma aplicación: la deserialización de
     * Java ejecuta código del archivo, así que nunca se debe cargar un modelo que haya subido un usuario.
     */
    static ModeloEntrenado leer(Path ruta) throws IOException, ClassNotFoundException {
        try (InputStream in = Files.newInputStream(ruta); ObjectInputStream ois = new LectorDeModelo(in)) {
            return (ModeloEntrenado) ois.readObject();
        }
    }

    /**
     * Resuelve las clases con el cargador de la propia aplicación. Con DevTools las clases del proyecto las carga un
     * cargador de reinicio distinto del de las bibliotecas, y la lectura por defecto no las encontraría.
     */
    private static final class LectorDeModelo extends ObjectInputStream {
        LectorDeModelo(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass descripcion) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(descripcion.getName(), false, ModeloEntrenado.class.getClassLoader());
            } catch (ClassNotFoundException ex) {
                return super.resolveClass(descripcion);
            }
        }
    }

    // ------------------------------------------------------------------ auxiliares

    /** Instala un modelo ya construido (lo usan las pruebas y la carga desde disco). */
    void instalar(ModeloEntrenado m) {
        this.modelo = m;
    }

    private ModeloEntrenado modeloActual() {
        ModeloEntrenado m = modelo;
        if (m == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El modelo de minería de datos aún no se ha entrenado. Un administrador debe entrenarlo primero.");
        }
        return m;
    }

    private static ModeloResponse respuesta(ModeloEntrenado m) {
        return new ModeloResponse(true, m.entrenadoEn(), ALGORITMO, m.metricas());
    }

    /** Weka devuelve NaN cuando una métrica no se puede calcular; JSON no admite NaN. */
    private static double limpio(double valor) {
        return Double.isNaN(valor) || Double.isInfinite(valor) ? 0.0 : valor;
    }
}
