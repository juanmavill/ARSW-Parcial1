package com.escuelaing.concurrencia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Turnos extends Thread {

	private static final Pattern TICKET_ID = Pattern.compile("\"ticketId\"\\s*:\\s*\"([^\"]+)\"");

	private static HttpClient clienteHttp;
	private static String urlBase;
	private static int creados = 0;
	private static int errores = 0;
	private static long sumaTiempos = 0;
	private static final List<String> ids = new ArrayList<>();

	public static void main(String[] args) throws Exception {
		urlBase = args.length > 0 ? args[0] : "http://127.0.0.1:8080";
		if (urlBase.endsWith("/")) {
			urlBase = urlBase.substring(0, urlBase.length() - 1);
		}

		int cantidad = args.length > 1 ? Integer.parseInt(args[1]) : 10;

		clienteHttp = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

		List<Turnos> hilos = new ArrayList<>();
		long inicioTotal = System.currentTimeMillis();

		for (int i = 0; i < cantidad; i++) {
			Turnos hilo = new Turnos();
			hilos.add(hilo);
			hilo.start();
		}

		for (Turnos hilo : hilos) {
			hilo.join();
		}

		long tiempoTotal = System.currentTimeMillis() - inicioTotal;
		double promedio = cantidad == 0 ? 0 : (double) sumaTiempos / cantidad;

		System.out.println("Tickets creados: " + creados);
		System.out.println("Errores: " + errores);
		System.out.println("Tiempo total ms: " + tiempoTotal);
		System.out.println("Tiempo promedio por solicitud ms: " + promedio);
		System.out.println("Ticket IDs: " + ids);
	}

	@Override
	public void run() {
		long inicio = System.currentTimeMillis();

		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(urlBase + "/api/tickets"))
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"service\":\"General\"}"))
				.build();

			HttpResponse<String> response = clienteHttp.send(request, HttpResponse.BodyHandlers.ofString());
			long tiempo = System.currentTimeMillis() - inicio;

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				String ticketId = sacarTicketId(response.body());
				if (ticketId != null) {
					guardarResultado(true, ticketId, tiempo);
				} else {
					guardarResultado(false, null, tiempo);
				}
			} else {
				guardarResultado(false, null, tiempo);
			}
		} catch (IOException e) {
			long tiempo = System.currentTimeMillis() - inicio;
			guardarResultado(false, null, tiempo);
		} catch (InterruptedException e) {
			long tiempo = System.currentTimeMillis() - inicio;
			guardarResultado(false, null, tiempo);
			Thread.currentThread().interrupt();
		}
	}

	private static synchronized void guardarResultado(boolean ok, String ticketId, long tiempo) {
		sumaTiempos = sumaTiempos + tiempo;

		if (ok && ticketId != null) {
			creados = creados + 1;
			ids.add(ticketId);
		} else {
			errores = errores + 1;
		}
	}

	private String sacarTicketId(String body) {
		Matcher matcher = TICKET_ID.matcher(body);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
}
