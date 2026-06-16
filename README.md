# Parcial ARSW T1 2026-i: Sistema de Turnos en Tiempo Real

## Funcionalidad

- Crear turnos para el servicio `General` con estado `CREATED`.
- Llamar el siguiente turno pendiente por API.
- Publicar eventos en tiempo real con Server-Sent Events cuando un turno cambia a `CALLED`.
- Interfaz web minima.
- Cliente Java concurrente para simular creacion de N turnos.

## Ejecutar la aplicacion

.\mvnw.cmd spring-boot:run

La aplicacion queda disponible en:
http://localhost:8080


## API 

Crear turno:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/tickets -ContentType "application/json" -Body '{"service":"General"}'
```

Alternativa con `curl.exe` en PowerShell:

```powershell
curl.exe --% -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" -d "{\"service\":\"General\"}"
```

Llamar siguiente turno:

```powershell
curl.exe -X POST http://localhost:8080/api/tickets/call-next
```


Ver turno actual:

```powershell
curl.exe http://localhost:8080/api/tickets/current
```


Escuchar eventos SSE:

```powershell
curl.exe http://localhost:8080/api/tickets/stream
```

## Cliente concurrente

Con la aplicacion corriendo, en otra terminal ejecutar:
.\mvnw.cmd -DskipTests compile
java -cp target/classes com.escuelaing.concurrencia.Turnos http://localhost:8080 50

Esto generara 50 tickets.

El cliente reporta tickets creados, errores, tiempo total y tiempo promedio por solicitud.
#Arquiterctura
<img width="862" height="367" alt="image" src="https://github.com/user-attachments/assets/8a546576-d0e6-4fff-80b6-f4158559514c" />

