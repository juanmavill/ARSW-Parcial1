package com.escuelaing.concurrencia;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private int contador = 0;
	private Ticket turnoActual = null;

	private final List<Ticket> cola = new ArrayList<>();
	private final Map<String, Ticket> tickets = new HashMap<>();
	private final List<SseEmitter> clientes = new ArrayList<>();

	@PostMapping
	public synchronized Ticket create(@RequestBody(required = false) TicketRequest request) {
		String servicio = "General";
		if (request != null && request.getService() != null && !request.getService().isBlank()) {
			servicio = request.getService();
		}

		contador = contador + 1;
		String id = "T-" + contador;

		Ticket ticket = new Ticket();
		ticket.setTicketId(id);
		ticket.setService(servicio);
		ticket.setStatus(TicketStatus.CREATED);
		ticket.setCreatedAt(Instant.now());

		tickets.put(id, ticket);
		cola.add(ticket);

		return ticket;
	}

	@GetMapping("/{ticketId}")
	public synchronized ResponseEntity<Ticket> findById(@PathVariable String ticketId) {
		Ticket ticket = tickets.get(ticketId);
		if (ticket == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(ticket);
	}

	@GetMapping("/current")
	public synchronized ResponseEntity<Ticket> current() {
		if (turnoActual == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(turnoActual);
	}

	@PostMapping("/call-next")
	public ResponseEntity<TicketEvent> callNext() {
		Ticket ticket;

		synchronized (this) {
			if (cola.isEmpty()) {
				return ResponseEntity.noContent().build();
			}

			ticket = cola.remove(0);
			ticket.setStatus(TicketStatus.CALLED);
			ticket.setCalledAt(Instant.now());
			turnoActual = ticket;
		}

		TicketEvent evento = new TicketEvent();
		evento.setType("TICKET_CALLED");
		evento.setTicket(ticket);
		enviarEvento(evento);

		return ResponseEntity.ok(evento);
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() throws IOException {
		SseEmitter cliente = new SseEmitter(0L);

		synchronized (clientes) {
			clientes.add(cliente);
		}

		cliente.onCompletion(() -> quitarCliente(cliente));
		cliente.onTimeout(() -> quitarCliente(cliente));
		cliente.onError(error -> quitarCliente(cliente));

		cliente.send(SseEmitter.event().name("CONNECTED").data(Map.of("message", "connected")));

		Ticket actual;
		synchronized (this) {
			actual = turnoActual;
		}

		if (actual != null) {
			TicketEvent evento = new TicketEvent();
			evento.setType("TICKET_CALLED");
			evento.setTicket(actual);
			cliente.send(SseEmitter.event().name("TICKET_CALLED").data(evento));
		}

		return cliente;
	}

	private void enviarEvento(TicketEvent evento) {
		List<SseEmitter> copiaClientes;

		synchronized (clientes) {
			copiaClientes = new ArrayList<>(clientes);
		}

		for (SseEmitter cliente : copiaClientes) {
			try {
				cliente.send(SseEmitter.event().name(evento.getType()).data(evento));
			} catch (IOException | IllegalStateException ex) {
				quitarCliente(cliente);
			}
		}
	}

	private void quitarCliente(SseEmitter cliente) {
		synchronized (clientes) {
			clientes.remove(cliente);
		}
	}

	public static class TicketRequest {
		private String service;

		public TicketRequest() {
		}

		public TicketRequest(String service) {
			this.service = service;
		}

		public String getService() {
			return service;
		}

		public void setService(String service) {
			this.service = service;
		}
	}

	public static class TicketEvent {
		private String type;
		private Ticket ticket;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Ticket getTicket() {
			return ticket;
		}

		public void setTicket(Ticket ticket) {
			this.ticket = ticket;
		}
	}

	public static class Ticket {
		private String ticketId;
		private String service;
		private TicketStatus status;
		private Instant createdAt;
		private Instant calledAt;

		public String getTicketId() {
			return ticketId;
		}

		public void setTicketId(String ticketId) {
			this.ticketId = ticketId;
		}

		public String getService() {
			return service;
		}

		public void setService(String service) {
			this.service = service;
		}

		public TicketStatus getStatus() {
			return status;
		}

		public void setStatus(TicketStatus status) {
			this.status = status;
		}

		public Instant getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Instant createdAt) {
			this.createdAt = createdAt;
		}

		public Instant getCalledAt() {
			return calledAt;
		}

		public void setCalledAt(Instant calledAt) {
			this.calledAt = calledAt;
		}
	}

	public enum TicketStatus {
		CREATED,
		CALLED
	}
}
