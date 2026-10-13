# Contrato: mensajes del operador

La app depende de los formatos de ETECSA. Este es el contrato que debe cumplir.

## Códigos que marca la app

| Acción | Código | SIM |
|---|---|---|
| Comprar Plan Diario | `*133*1*3#` | la elegida en la app |
| Comprar Plan 35 días `index` N (1–4) | `*133*1*4*N#` | la elegida en la app |
| Saldo de datos | `*222*328#` | la elegida en la app |
| Bonos y datos .cu | `*222*266#` | la elegida en la app |
| Saldo de una línea empresarial (no puede comprar planes) | `*111#` (en lugar de los dos anteriores) | la elegida en la app |

## Respuestas que interpreta

| Texto (ejemplo real) | Resultado |
|---|---|
| `Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#` | International = 0, sin error |
| `Tarifa: No activa. Datos: 130.08 MB validos 24 dias.` | International = 130.08 MB, vence en 24 días |
| `Tarifa: No activa. Diaria: 200 MB no activos.` | DailyBag = 200 MB, sin vencimiento hasta el primer uso |
| `Voz: 148 MIN. SMS: 8. Datos: 16700 MB. Roaming:` (`*111#`) | International = 16700 MB, sin vencimiento |
| `Datos.cu 273 MB vence 06-11-26.` | National = 273 MB, vence 06-11-2026 23:59:59 |

## SMS de compra

- Remitentes aceptados: `cubacel` y `ETECSA` (sin distinguir mayúsculas).
- Se busca el `smsKey` de cada plan en el texto normalizado (sin espacios, sin mayúsculas).
- Ejemplos verificados (texto tras "Se ha comprado"): `Plan 4.5GB` → Plan 4.5 GB; `Plan 2GB+15MIN+20SMS` →
  Plan 120; `Plan 4GB+35MIN+40SMS` → Plan 240; `Plan 6GB+60MIN+70SMS` → Plan 360; `Plan Diario 200MB` → Plan
  Diario.
- La comparación usa la clave completa, para no confundir planes cuyas claves empiezan igual.
