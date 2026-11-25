# Colección de Postman — Patent Sweep

| Archivo | Contenido |
|---|---|
| `patent-sweep.postman_collection.json` | 17 peticiones con pruebas: salud, login, HU-02, HU-04 y rutas inválidas |
| `local.postman_environment.json` | `baseUrl = http://localhost:8085` |
| `produccion.postman_environment.json` | `baseUrl = https://patent-sweep.inmemorialake.dev` |

## Uso

1. En Postman: **Import** → seleccione los tres archivos.
2. Elija el entorno (arriba a la derecha) y escriba `username` y `password` **en el entorno**:
   son `ADMIN_USUARIO` / `ADMIN_CLAVE` del `.env` (el local o el del servidor). Nunca los guarde en la
   colección ni los suba al repositorio.
3. Ejecute **Autenticación > Login**: guarda el token en la variable `token` y el resto de peticiones lo
   envían como Bearer. Si el token vence (`JWT_EXPIRACION_MINUTOS`), vuelva a ejecutar el login.
4. Para correr todo: clic derecho en la colección → **Run collection**, en el orden en que está.

## Notas

- Los resultados esperados salen de las APIs simuladas (`patent-sweep/mocks`), que también se usan en producción.
- **Con coincidencias (form-001)** guarda conflictos e historial en la base del entorno. Repetirla no duplica
  conflictos (unicidad por fórmula + patente).
- **Estado con alertas (form-001)** espera que antes se haya ejecutado la búsqueda de `form-001` o un barrido.
- Las búsquedas manuales exigen rol `ADMINISTRADOR`; con una cuenta `SERVICIO` responden 403.
