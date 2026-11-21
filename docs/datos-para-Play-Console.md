# Tlapohua 0.31 y Seguridad de los datos

Documento técnico preparado el 15 de noviembre de 2026. La declaración se completa y envía desde la cuenta del responsable en Play Console.

## Comportamiento verificado en el código

| Función | Datos y destino |
|---|---|
| Partidas | Respuestas, candidatos, provisionales, tiempos, errores e historial almacenados en el teléfono. |
| Liga de amigos | Apodo, UUID generado por la app, puntos, rango, colección, tableros de Cumbre, mejores tiempos y fecha. Se envían cuando el usuario exporta o confirma un intercambio Bluetooth. |
| Bluetooth | La app solicita Dispositivos cercanos en Android reciente para comunicarse con un teléfono previamente vinculado. El intercambio exige confirmar un código en ambos teléfonos. |
| Logo | La imagen elegida se copia en el almacenamiento privado de la app. |
| Reportes y PDFs | El usuario elige dónde guardar los archivos mediante el selector de Android. |
| Respaldos | El usuario puede exportar el progreso, ajustes y fichas. El archivo está comprimido y carece de cifrado propio. El logo personal se elige nuevamente en el teléfono de destino. |
| Recuperación interrumpida | La app guarda temporalmente una copia privada del progreso anterior y la elimina cuando termina la restauración o recuperación. |
| Internet y bibliotecas | El manifiesto carece del permiso de Internet. El código incorpora APIs de Android, sin SDK publicitario ni analítica remota. |

El usuario puede quitar fichas individualmente, cambiar el apodo y quitar el logo personal. Borrar los datos de la app desde Android elimina el almacenamiento local. Las copias exportadas y las recibidas por amigos se gestionan desde sus respectivos destinos.

## Cómo usar esta información al completar el formulario

Google distingue el procesamiento exclusivamente local de la recogida de datos que salen del dispositivo. También contempla excepciones a la declaración de uso compartido para transferencias iniciadas por el usuario. Hay que revisar la aplicación concreta de esas definiciones a las exportaciones y a Bluetooth antes de enviar las respuestas del formulario. La ausencia de permiso de Internet, por sí sola, no determina todas las respuestas. [Definiciones de Seguridad de los datos](https://support.google.com/googleplay/android-developer/answer/10787469)

No se debe afirmar que todos los archivos exportados están cifrados. El formato de respaldo y de ficha permite recuperar los datos a quien posea el archivo.

## Información necesaria del responsable

- Nombre público que coincida con la ficha de desarrollador.
- Contacto de privacidad.
- Dirección web pública y accesible de la política.
- Público y edades objetivo.

La política debe identificar al responsable y al contacto, explicar tratamiento, conservación y eliminación, y estar disponible desde la app y desde la ficha. [Política de datos de usuario de Google Play](https://support.google.com/googleplay/android-developer/answer/10144311)

## Pruebas todavía necesarias en teléfonos

La compilación y las pruebas automatizadas verifican el contenido de las descripciones accesibles, los datos del respaldo y la recuperación por transacción simulada. Falta revisar TalkBack en una partida, texto ampliado, desplazamiento del tablero, interrupciones reales del proceso y Bluetooth entre dos teléfonos.

Los perfiles de división se verifican por tamaños y operaciones. La dificultad percibida debe calibrarse con jugadores y tiempos de resolución.
