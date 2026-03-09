# touche
Sistema de almacenamiento distribuido sencillo.

# Distribución de clases
## Sistema
- De forma general el sistema acciona de la siguiente manera:
    * Se espera que se formen dos anillos, el anillo A contendrá:
        + Los líderes, el principal y sus posibles sucesores.
        + El umbral de máximo número de líderes se mantiene determinado de manera estática.
	* El anillo B contendrá:
		+  Todos los nodos trabajadores.
- El accionar del sistema dependerá del voto de la mayoría de los nodos, solo de esa manera se hará o no se hará algún cambio.
- Lo anterior se gestiona mediante un servicio de consenso `QuorumManager`.

- Para la subida/carga de archivos se hará de la siguiente manera:
    * Subiendo los archivos a un directorio llamado 'archivos_entrada', este funge como 'cola' de archivos a integrar al sistema distribuido, debido a la carencia de alguna shell CLI o GUI con la que interactuar.
    * Por eso existe el servicio FileWatcherService para detectar los cambios en el directorio.
    * Una vez detectado el cambio se solicita la carga al sistema, el líder/lideres debe elegir que nodo debe ser el que lo vaya a alojar en su almacenamiento.
    * Si se decide que se puede cargar y se asigna a un nodo, entonces se acciona el proceso para enviarlo a través de la red, debido a la manera en la que funciona gRPC se deberá dividir el archivo en varios fragmentos (por defecto se particionará en trozos de 2 MB).
    * Los fragmentos deberán llegar al almacenamiento del nodo objetivo (el elegido) en un directorio llamado por defecto como 'archivos', acá una vez que hayan llegado todos los fragmentos se deberán de reestructurar para dejar al archivo como uno solo.
    * Una vez hecho esto entonces se marcará como completado y se añadirá a la lista de archivos del sistema, que deberá ser un objeto de tipo Map en Java para facilitar su búsqueda e indizado en el sistema.
    * Si se activa réplica (por defecto se dejará con un número de dos réplicas, para probar) entonces se deberá elegir un segundo nodo al que enviar ese archivo, también se deberá registrar en la lista de archivos del sistema.
    
- Para que todos los nodos del sistema sepan el estado de todos los archivos y de los nodos en sí mismos, deberá de accionarse el servicio de sincronización, SyncStateService, cada que se haya hecho una escritura/réplica o haya cambios en la estructura de los anillos, que todos sepan el estado de los demás facilitará algunos de los siguientes procedimientos:
    * Elección de nodos para averigüar quienes serán lideres basados en los ID, probablemente mediante el algoritmo Bully, para facilitarlo.
    * Elección del mejor nodo de acuerdo al número de archivos que aloja cada uno.
    * Balanceo de carga cada cierto tiempo, buscando que la carga esté lo más equitativemente repartida posible.
    
### Características importantes:

1. Conexiones
- El descubrimiento de los nodos se deberá hacer mediante UDP Broadcast mandando una señal 'ping', los demás nodos se mantienen siempre en escucha en algún hilo para poder reconocer otros nodos y poder así integrarlos.
- Una vez que se reconocen se comparten la información acerca de sus direcciones (IP:Puerto) para poder hacer una comunicación directa mediante TCP usando gRPC para llevar a cabo la ejecución de código remoto.
- Para facilitar el descubrimiento remoto se busca también que funcione a través de Tailscale en una Tailnet propiciando que no solo las computadoras dentro de la misma red puedan conectarse, sino que las que integren la Tailnet también puedan hacerlo, sin embargo, eso complicaría el descubrimiento mediante Broadcasting.

2. Detección de fallas
- Se debe accionar un sistema de latidos para con todos los nodos para poder detectar cuando alguno falla además de así poder reestructurar el sistema de anillos en caso de que haya más o menos nodos, por ejemplo se tiene que a partir de 2 nodos el sistema cree el segundo anillo y que los siguientes nodos a ingresar pasen a formar parte del anillo B.
3. Metadatos
- Para hacerlo más tolerante a fallos sería buena idea que los que lo mantuviesen fueran todos, que los cambios del Map fueran incrementales y que se sincronicen mediante el StateSyncService.
- En caso de que un nodo trabajador reciba los fragmentos de un archivo, debería mandar una señal al anillo A (posiblemente mediante Broadcast) para que se actualice el Map, si bien los nodos trabajadores tienen acceso al Map no deben poder modificarlo, solo los del anillo A.
4. Chunks
- Para saber cuando ya se terminó, debería de mandarse mediante un argumento el número de fragmentos que resultan de la división del tamaño total del archivo entre el valor en bytes de cada chunk, por lo que se debe ir comparando con un contador hasta que llegue al número exacto de chunks que llegaron, de esa manera sencilla se podría confirmar la escritura exitosa (sin usar métodos complejos de Hashing para confirmarlo de una manera más estricta).
5. Ingreso de nodos
- Se tiene ideado que todos los nodos escuchen en una dirección especifica para ingresar, al inicio se ingresa al anillo A, sin embargo debe bloquearse por algún tiempo tratando de encontrar a otros nodos mediante el envío de pings a la dirección en la que todos escuchan, si tras cierto tiempo no recibe señal de vida de otros nodos entonces se integra al anillo A (como único líder), en caso de encontrar otros nodos y que el umbral de máximo de número de integrantes del anillo A (líderes) se haya pasado entonces se asignará al Anillo B, se debe de desbloquear automáticamente tras ingresar a un anillo para poder así recibir peticiones de alojamiento.
6. Concurrencia
- El plan sería usar alguna cola de suministro bloqueante, en la que se puedan añadir nuevas peticiones pero se tomarán una a una para asegurar el correcto envío.

## Nodo
- Contiene un identificador numérico, este servirá para efectuar desempates mediante el algoritmo Bully en momentos en los que se tenga que elegir a un nodo, pero por algún motivo todos estén en igualdad de condiciones, por ejemplo:
    * Si al tratar de elegir al nodo más vacío para asignarle la el almacenamiento de un archivo nos encontramos con que no se puede elegir dado que están todos con el mismo valor de uso, supongamos dos nodos y que ambos tienen dos archivos, técnicamente están iguales, pero no se puede elegir de esa manera a ninguno, entonces la manera de desempatarlo es eligiendo al nodo con ID mayor.
- IP:Puerto, como identificador para que se haga el envío de mensajes a través de gRPC.
- Nombre, identificador en forma de cadena para hacer que sea más fácil de distinguirse en los logs.
- RolActual, este permite saber la manera en la que el Nodo debe de comportarse, como Líder o como Trabajador, el líder si bien también puede almacenar, también tiene las facultades de mandar ordenes.
- IDNodoActual, este indica el nombre del anillo al que el nodo pertenece, con esto se indica si se es un posible líder (Anillo A) o si se es un posible trabajador (Anillo B).
- El nodo puede mandar sus propuestas mediante la función `proponer()`.
- Se puede promover a líder mediante `promoteToLeader()`.
- Se puede convertir a trabajador con `demoteToWorker()`.