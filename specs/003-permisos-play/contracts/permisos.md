# Contrato: cuándo y cómo se pide cada permiso

La app expone al usuario (y a una revisión de Play) este comportamiento. Cada petición va precedida de su explicación
(`Permission.description` en `SinglePermissionFragment` o en la lista de "Permisos y privacidad").

| Permiso | Momento | Si se rechaza | Si se rechazó de forma permanente |
|---|---|---|---|
| Acceso de uso | Primer uso (tras la explicación de datos); pantallas de estadísticas | Estadísticas y cortafuegos dinámico muestran la explicación y "Conceder" | Abre el ajuste de acceso de uso (no tiene diálogo del sistema) |
| Notificaciones (13+) | Primer uso, paso opcional | La app funciona; la notificación de consumo no se ve | Abre los ajustes de la app |
| Llamadas y estado del teléfono | Configurar SIM, Sincronizar, Comprar, USSD del panel | La acción no se hace; la sincronización en segundo plano se omite sin avisar | Abre los ajustes de la app |
| Superposición | Activar la burbuja | La burbuja queda desactivada | Abre el ajuste de superposición |
| VPN | Activar el cortafuegos | El cortafuegos queda desactivado | Diálogo del sistema de VPN (no tiene rechazo permanente) |
| Accesibilidad | Sincronizar por el camino legacy (API < 26 o modo moderno desactivado), tras la divulgación con "Acepto" | La sincronización se cancela con un mensaje | Abre el ajuste de accesibilidad |
| Optimización de batería | Solo en "Permisos y privacidad", como recomendación | — | Abre la lista de optimización de batería |

## Primer uso

1. Explicación de datos: consumo por app, lista de apps instaladas (cortafuegos), procesamiento solo en el teléfono,
   únicas conexiones (actualizaciones de Apklis y envío de errores por correo que inicia el usuario).
2. Botones "Continuar" y "Ahora no". "Ahora no" no pide nada y lleva a la app.
3. "Continuar" muestra la lista con el acceso de uso y, en Android 13+, las notificaciones; cada uno con "Conceder";
   "Continuar" final lleva a la app aunque no se haya concedido nada.
4. Se muestra una sola vez (`DATA_DISCLOSURE_ANSWERED`), y no a quien ya tenía el acceso de uso concedido.

## SMS User Consent

- Se inicia al pulsar "Comprar"; escucha 5 minutos cualquier SMS de un remitente que no está en los contactos.
- El sistema muestra su diálogo para compartir ese mensaje; la app solo lee el texto si el usuario acepta.
- Solo se usa para confirmar la compra (ver [data-model.md](../data-model.md)); el texto no se guarda.
