package com.example.labmcp.service;

import com.example.labmcp.model.Tarea;
import com.example.labmcp.repository.TareaRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * CRUD de tareas expuesto como herramientas MCP.
 *
 * Cada método @Tool se publica automáticamente por el servidor MCP:
 *   - nombre      = nombre del método (o el atributo name)
 *   - descripción = atributo description
 *   - JSON schema      = derivado de los parámetros (incluye @ToolParam)
 *
 * Claude (desde Claude Desktop) podrá invocar estos métodos cuando el usuario
 * hable en lenguaje natural: "créame una tarea para el lunes", "márcalas todas
 * como completadas", etc.
 *
 * Cambios del laboratorio (actividades):
 *   - Actividad 1: herramienta contarTareas.
 *   - Actividad 2: herramienta listCompletadas.
 *   - Actividad 3: validación de fecha ISO yyyy-MM-dd en el servidor.
 *   - Hallazgo de la prueba con el Inspector: updateTarea guardaba el texto
 *     "null" como fecha y no recortaba espacios; ahora se normaliza la entrada.
 */
@Component
public class TareaToolsService {

    private final TareaRepository repo;

    public TareaToolsService(TareaRepository repo) {
        this.repo = repo;
    }

    // ---------- VALIDACIÓN (Actividad 3) ----------

    /**
     * Valida que la fecha tenga formato ISO estricto yyyy-MM-dd y sea una fecha real.
     * Devuelve la fecha ya recortada; lanza IllegalArgumentException si es inválida.
     */
    public static String validarFechaIso(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            throw new IllegalArgumentException("La fecha es obligatoria con formato yyyy-MM-dd (ej. 2026-11-20)");
        }
        String limpia = fecha.trim();
        try {
            LocalDate.parse(limpia, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Fecha inválida '" + limpia + "': use el formato ISO yyyy-MM-dd (ej. 2026-11-20)");
        }
        return limpia;
    }

    /** Un cliente MCP puede enviar "", " " o el texto "null" cuando quiere decir "sin valor". */
    private static boolean sinValor(String s) {
        return s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim());
    }

    // ---------- READ ----------

    @Tool(name = "listTareas", description = "Devuelve la lista completa de tareas almacenadas.")
    public List<Tarea> listarTareas() {
        return repo.findAll();
    }

    @Tool(name = "listPendientes", description = "Devuelve únicamente las tareas pendientes (completado = false).")
    public List<Tarea> listarPendientes() {
        return repo.findByCompletado(false);
    }

    // Actividad 2
    @Tool(name = "listCompletadas", description = "Devuelve únicamente las tareas ya completadas (completado = true).")
    public List<Tarea> listarCompletadas() {
        return repo.findByCompletado(true);
    }

    // Actividad 1
    @Tool(name = "contarTareas", description = "Devuelve el número total de tareas almacenadas (pendientes y completadas). Úsala cuando el usuario pregunte cuántas tareas hay.")
    public Long contarTareas() {
        return repo.count();
    }

    @Tool(name = "buscarTareas", description = "Busca tareas cuyo título contiene el fragmento indicado (case-insensitive).")
    public List<Tarea> buscarTareas(
            @ToolParam(description = "Fragmento de texto a buscar en el título") String fragmento) {
        return repo.findByTituloContainingIgnoreCase(fragmento);
    }

    // ---------- CREATE ----------

    @Tool(name = "crearTarea", description = "Crea una nueva tarea con el título y fecha indicados. La fecha debe tener formato ISO yyyy-MM-dd. Por defecto queda como pendiente.")
    @Transactional
    public Tarea crearTarea(
            @ToolParam(description = "Título descriptivo de la tarea") String titulo,
            @ToolParam(description = "Fecha en formato ISO yyyy-MM-dd (p.ej. 2026-11-20)") String fecha) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        String fechaValida = validarFechaIso(fecha);
        Tarea t = new Tarea(titulo.trim(), fechaValida, Boolean.FALSE);
        return repo.save(t);
    }

    // ---------- UPDATE ----------

    @Tool(name = "markComplete", description = "Marca como completada la tarea con el id indicado.")
    @Transactional
    public Tarea marcarCompletada(
            @ToolParam(description = "Identificador único de la tarea") Long id) {
        Tarea t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la tarea " + id));
        t.setCompletado(Boolean.TRUE);
        return repo.save(t);
    }

    @Tool(name = "updateTarea", description = "Actualiza el título o la fecha (formato yyyy-MM-dd) de una tarea existente. Los parámetros nulos o vacíos no se modifican.")
    @Transactional
    public Tarea actualizarTarea(
            @ToolParam(description = "Id de la tarea a actualizar") Long id,
            @ToolParam(description = "Nuevo título (opcional, null = no cambiar)") String titulo,
            @ToolParam(description = "Nueva fecha yyyy-MM-dd (opcional, null = no cambiar)") String fecha) {
        Tarea t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la tarea " + id));
        if (!sinValor(titulo)) t.setTitulo(titulo.trim());
        if (!sinValor(fecha))  t.setFecha(validarFechaIso(fecha));
        return repo.save(t);
    }

    // ---------- DELETE ----------

    @Tool(name = "eliminarTarea", description = "Elimina la tarea con el id indicado. Devuelve un mensaje de confirmación.")
    @Transactional
    public String eliminarTarea(
            @ToolParam(description = "Id de la tarea a eliminar") Long id) {
        if (!repo.existsById(id)) {
            return "La tarea " + id + " no existe.";
        }
        repo.deleteById(id);
        return "Tarea " + id + " eliminada correctamente.";
    }
}
