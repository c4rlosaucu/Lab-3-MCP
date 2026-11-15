package com.example.labmcp.service;

import com.example.labmcp.model.Tarea;
import com.example.labmcp.repository.TareaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Actividad 6: pruebas automatizadas de la lógica de negocio (sin base de datos:
 * el repositorio se reemplaza por un mock de Mockito).
 */
@ExtendWith(MockitoExtension.class)
class TareaToolsServiceTest {

    @Mock
    private TareaRepository repo;

    private TareaToolsService service;

    @BeforeEach
    void setUp() {
        service = new TareaToolsService(repo);
    }

    @Test
    void contarTareas_devuelveElTotalDelRepositorio() {
        when(repo.count()).thenReturn(5L);
        assertEquals(5L, service.contarTareas());
    }

    @Test
    void listarCompletadas_consultaSoloLasCompletadas() {
        Tarea hecha = new Tarea("Probar demo", "2026-11-09", true);
        when(repo.findByCompletado(true)).thenReturn(List.of(hecha));

        List<Tarea> resultado = service.listarCompletadas();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getCompletado());
    }

    @Test
    void crearTarea_guardaPendienteConTituloRecortado() {
        when(repo.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Tarea creada = service.crearTarea("  Estudiar MCP  ", "2026-11-20");

        ArgumentCaptor<Tarea> captor = ArgumentCaptor.forClass(Tarea.class);
        verify(repo).save(captor.capture());
        assertEquals("Estudiar MCP", captor.getValue().getTitulo());
        assertEquals("2026-11-20", captor.getValue().getFecha());
        assertFalse(captor.getValue().getCompletado());
        assertEquals("Estudiar MCP", creada.getTitulo());
    }

    @Test
    void crearTarea_conFechaNoIso_lanzaExcepcionYNoGuarda() {
        assertThrows(IllegalArgumentException.class, () -> service.crearTarea("Tarea", "20/11/2026"));
        verify(repo, never()).save(any());
    }

    @Test
    void crearTarea_conTituloVacio_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> service.crearTarea("   ", "2026-11-20"));
        verify(repo, never()).save(any());
    }

    @Test
    void actualizarTarea_conTextoNull_noCambiaLaFecha() {
        Tarea existente = new Tarea("Original", "2026-11-15", false);
        when(repo.findById(4L)).thenReturn(Optional.of(existente));
        when(repo.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Tarea actualizada = service.actualizarTarea(4L, "Nuevo título ", "null");

        assertEquals("Nuevo título", actualizada.getTitulo());
        assertEquals("2026-11-15", actualizada.getFecha());
    }

    @Test
    void actualizarTarea_conFechaInvalida_lanzaExcepcion() {
        Tarea existente = new Tarea("Original", "2026-11-15", false);
        when(repo.findById(4L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class, () -> service.actualizarTarea(4L, null, "mañana"));
        verify(repo, never()).save(any());
    }

    @Test
    void validarFechaIso_aceptaFechasRealesYRechazaLasDemas() {
        assertEquals("2026-11-20", TareaToolsService.validarFechaIso(" 2026-11-20 "));
        assertThrows(IllegalArgumentException.class, () -> TareaToolsService.validarFechaIso("2026-02-30"));
        assertThrows(IllegalArgumentException.class, () -> TareaToolsService.validarFechaIso("2026/11/20"));
        assertThrows(IllegalArgumentException.class, () -> TareaToolsService.validarFechaIso("null"));
        assertThrows(IllegalArgumentException.class, () -> TareaToolsService.validarFechaIso(null));
    }
}
