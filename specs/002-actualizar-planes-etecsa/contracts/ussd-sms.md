# Contrato: mensajes del operador

La app depende de los formatos de ETECSA. Este es el contrato que debe cumplir.

## Códigos que marca la app

| Acción | Código | SIM |
|---|---|---|
| Comprar Plan Diario | `*133*1*3#` | la elegida en la app |
| Comprar Plan 35 días `index` N (1–4) | `*133*1*4*N#` | la elegida en la app |
| Saldo de datos | `*222*328#` | la elegida en la app |
| Bonos y datos .cu | `*222*266#` | la elegida en la app |

## Respuestas que interpreta

| Texto (ejemplo real) | Resultado |
|---|---|
| `Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#` | International = 0, sin error |
| `Tarifa: No activa. Datos: 130.08 MB validos 24 dias.` | International = 130.08 MB, vence en 24 días |
| `Datos.cu 273 MB vence 06-11-26.` | National = 273 MB, vence 06-11-2026 23:59:59 |

## SMS de compra

- Remitentes aceptados: `cubacel` y `ETECSA` (sin distinguir mayúsculas).
- Se busca el `smsKey` de cada plan en el texto normalizado (sin espacios, sin mayúsculas).
- Ejemplo verificado: `ETECSA Plan Nacional: Se ha comprado Plan 2GB+15MIN+20SMS valido por 35 dias, ...`
  → compra del Plan 120.
