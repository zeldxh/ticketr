package com.example.sistemaboletos;

import com.example.sistemaboletos.model.Compra;
import com.example.sistemaboletos.model.Evento;
import com.example.sistemaboletos.model.Rol;
import com.example.sistemaboletos.model.Usuario;
import com.example.sistemaboletos.repository.CompraRepository;
import com.example.sistemaboletos.repository.EventoRepository;
import com.example.sistemaboletos.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automatiza con Selenium WebDriver los casos de prueba de G10_SC405_LN_TestCases.xlsx
 * que fueron disenados como manuales en el Avance 2, pero que son tecnicamente
 * automatizables por tener un resultado esperado deterministico y estable:
 * TC-01, TC-03, TC-04, TC-05, TC-06, TC-07, TC-08, TC-10, TC-11 y TC-12.
 *
 * Quedan fuera de esta automatizacion, con justificacion documentada en el
 * Cierre y Defensa (seccion "Casos Mejorables y Recomendaciones"):
 * - TC-02: depende de que primero se corrija BUG-01 (correo duplicado);
 *   automatizarlo hoy generaria una prueba fragil contra un resultado inestable.
 * - TC-09: necesita que primero se agregue confirmacion de interfaz antes de
 *   eliminar un evento, para no automatizar un flujo que podria cambiar.
 * - TC-13: automatizado por separado en Tc13CompraConcurrenteSeleniumTest,
 *   por depender de dos solicitudes simultaneas, un escenario distinto al de
 *   este archivo (que usa un unico navegador por caso).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CasosFuncionalesSeleniumTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "Selenium123!";

    private WebDriver driver;
    private final List<Integer> usuariosCreados = new ArrayList<>();
    private final List<Integer> eventosCreados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1280,900",
                "--no-sandbox", "--disable-dev-shm-usage", "--disable-extensions",
                "--disable-background-networking", "--disable-notifications");
        driver = new ChromeDriver(options);
        // Espera implicita: al correr toda la bateria de pruebas seguida, Chrome en
        // modo headless puede tardar un poco mas en renderizar cada pagina nueva.
        // Sin esto, findElement() falla intermitentemente por pura carga del sistema,
        // no por un error real de la aplicacion.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        for (Integer eventoId : eventosCreados) {
            compraRepository.findAll().stream()
                    .filter(c -> c.getEvento() != null && eventoId.equals(c.getEvento().getId()))
                    .forEach(c -> compraRepository.deleteById(c.getId()));
            eventoRepository.deleteById(eventoId);
        }
        for (Integer usuarioId : usuariosCreados) {
            usuarioRepository.deleteById(usuarioId);
        }
    }

    // ---------- TC-01: registro de usuario nuevo con datos validos ----------
    @Test
    void tc01RegistroDeUsuarioNuevoConDatosValidos() {
        String email = correoUnico("tc01");
        driver.get(baseUrl() + "/registro");
        driver.findElement(By.id("nombre")).sendKeys("Usuario TC-01 Selenium");
        driver.findElement(By.id("email")).sendKeys(email);
        WebElement password = driver.findElement(By.id("password"));
        password.sendKeys(PASSWORD);
        clickSubmitDelFormularioDe(password);

        assertTrue(driver.getCurrentUrl().contains("/login"), "Debe redirigir a /login tras registrar");
        assertTrue(driver.getCurrentUrl().contains("registro_exitoso"), "Debe indicar registro_exitoso");

        Optional<Usuario> creado = usuarioRepository.findByEmail(email);
        assertTrue(creado.isPresent(), "El usuario debe quedar persistido");
        assertEquals(Rol.USUARIO, creado.get().getRol(), "El rol por defecto debe ser USUARIO");
        usuariosCreados.add(creado.get().getId());
    }

    // ---------- TC-03: inicio de sesion exitoso y redireccion a eventos ----------
    @Test
    void tc03InicioDeSesionExitosoYRedireccionAEventos() {
        Usuario usuario = crearUsuario(correoUnico("tc03"), Rol.USUARIO);
        driver.get(baseUrl() + "/login");
        driver.findElement(By.id("username")).sendKeys(usuario.getEmail());
        WebElement password = driver.findElement(By.id("password"));
        password.sendKeys(PASSWORD);
        clickSubmitDelFormularioDe(password);

        assertTrue(driver.getCurrentUrl().contains("/eventos"), "Debe redirigir a /eventos tras login exitoso");
        assertTrue(driver.getCurrentUrl().contains("bienvenida"), "Debe incluir el parametro bienvenida");
    }

    // ---------- TC-04: credenciales incorrectas con mensaje generico ----------
    @Test
    void tc04CredencialesIncorrectasConMensajeGenerico() {
        Usuario usuario = crearUsuario(correoUnico("tc04"), Rol.USUARIO);
        driver.get(baseUrl() + "/login");
        driver.findElement(By.id("username")).sendKeys(usuario.getEmail());
        WebElement password = driver.findElement(By.id("password"));
        password.sendKeys("contrasena-incorrecta");
        clickSubmitDelFormularioDe(password);

        assertTrue(driver.getCurrentUrl().contains("/login"));
        assertTrue(driver.getCurrentUrl().contains("error=true"), "Debe redirigir a /login?error=true");
        assertTrue(driver.getPageSource().contains("Usuario o contrase"),
                "Debe mostrar el mensaje generico de credenciales incorrectas");
    }

    // ---------- TC-05: restriccion de rutas administrativas para USUARIO ----------
    @Test
    void tc05RestriccionDeRutasAdministrativasParaUsuarioRegular() {
        Usuario usuario = crearUsuario(correoUnico("tc05"), Rol.USUARIO);
        iniciarSesion(usuario.getEmail());
        driver.get(baseUrl() + "/admin");

        // SecurityConfig usa .accessDeniedPage("/access-denied"), que Spring Security
        // resuelve con un FORWARD del lado del servidor, no un redirect: la URL del
        // navegador se mantiene en /admin aunque el contenido mostrado sea el de
        // acceso denegado. Por eso se verifica el contenido de la pagina, no la URL.
        assertTrue(driver.getPageSource().contains("Acceso Denegado"),
                "Un usuario con rol USUARIO no debe poder acceder a /admin");
    }

    // ---------- TC-06: acceso permitido al panel administrativo para ADMIN ----------
    @Test
    void tc06AccesoPermitidoAlPanelAdministrativoParaAdmin() {
        Usuario admin = crearUsuario(correoUnico("tc06"), Rol.ADMIN);
        iniciarSesion(admin.getEmail());
        driver.get(baseUrl() + "/admin");
        pausaDeSincronizacion();

        assertTrue(driver.getCurrentUrl().endsWith("/admin"), "El administrador debe poder abrir /admin");
        assertTrue(!driver.getCurrentUrl().contains("access-denied") && !driver.getCurrentUrl().contains("/login"));
    }

    // ---------- TC-07: creacion de evento con datos validos ----------
    @Test
    void tc07CreacionDeEventoConDatosValidos() {
        Usuario admin = crearUsuario(correoUnico("tc07"), Rol.ADMIN);
        iniciarSesion(admin.getEmail());

        String nombreEvento = "Evento TC-07 Selenium " + UUID.randomUUID();
        driver.get(baseUrl() + "/admin/eventos/nuevo");
        driver.findElement(By.id("nombre")).sendKeys(nombreEvento);
        driver.findElement(By.id("descripcion")).sendKeys("Evento creado por la prueba automatizada TC-07.");
        driver.findElement(By.id("lugar")).sendKeys("Auditorio Automatizacion");
        setFechaHora(driver.findElement(By.id("fecha")), "2027-01-15T18:00");
        driver.findElement(By.id("capacidad")).sendKeys("50");
        WebElement precio = driver.findElement(By.id("precio"));
        precio.sendKeys("15000");
        clickSubmitDelFormularioDe(precio);

        assertTrue(driver.getCurrentUrl().contains("/admin/eventos"));
        List<Evento> encontrados = eventoRepository.findAll().stream()
                .filter(e -> nombreEvento.equals(e.getNombre())).toList();
        assertEquals(1, encontrados.size(), "El evento debe quedar creado exactamente una vez");
        assertEquals(50, encontrados.get(0).getBoletosDisponibles(),
                "boletosDisponibles debe igualar la capacidad cuando no se especifica aparte");
        eventosCreados.add(encontrados.get(0).getId());
    }

    // ---------- TC-08: edicion de informacion de un evento existente ----------
    @Test
    void tc08EdicionDeInformacionDeEventoExistente() {
        Usuario admin = crearUsuario(correoUnico("tc08"), Rol.ADMIN);
        Evento evento = crearEvento("Evento TC-08 Original", 10);
        iniciarSesion(admin.getEmail());

        String nuevoNombre = "Evento TC-08 Editado " + UUID.randomUUID();
        driver.get(baseUrl() + "/admin/eventos/editar/" + evento.getId());

        // Hallazgo durante la automatizacion: el campo Fecha y Hora del formulario de
        // edicion no llega prellenado por Thymeleaf (queda vacio), y al ser obligatorio
        // el navegador bloquea el envio del formulario de forma nativa. Se establece
        // aqui explicitamente para poder guardar, y se deja documentado como hallazgo
        // de UX en el material de estudio (el mismo patron ya se habia observado y
        // reportado para este formulario).
        setFechaHora(driver.findElement(By.id("fecha")), "2027-02-20T19:00");

        WebElement campoNombre = driver.findElement(By.id("nombre"));
        campoNombre.clear();
        campoNombre.sendKeys(nuevoNombre);
        clickSubmitDelFormularioDe(campoNombre);
        pausaDeSincronizacion();

        Evento actualizado = eventoRepository.findById(evento.getId()).orElseThrow();
        assertEquals(nuevoNombre, actualizado.getNombre(), "El listado debe reflejar el nombre actualizado");
    }

    // ---------- TC-10: listado publico de eventos con disponibilidad ----------
    @Test
    void tc10ListadoPublicoDeEventosConDisponibilidad() {
        Evento evento = crearEvento("Evento TC-10 Publico " + UUID.randomUUID(), 20);
        driver.get(baseUrl() + "/eventos");

        assertTrue(driver.getPageSource().contains(evento.getNombre()),
                "El listado publico debe mostrar el evento sin necesidad de autenticacion");
    }

    // ---------- TC-11: compra valida por usuario autenticado ----------
    @Test
    void tc11CompraValidaPorUsuarioAutenticado() {
        Usuario usuario = crearUsuario(correoUnico("tc11"), Rol.USUARIO);
        Evento evento = crearEvento("Evento TC-11 Compra " + UUID.randomUUID(), 5);
        iniciarSesion(usuario.getEmail());

        driver.get(baseUrl() + "/eventos/" + evento.getId());
        WebElement cantidad = driver.findElement(By.id("cantidad"));
        cantidad.sendKeys("2");
        clickSubmitDelFormularioDe(cantidad);

        assertTrue(driver.getCurrentUrl().contains("compra_exitosa"));
        Evento eventoFinal = eventoRepository.findById(evento.getId()).orElseThrow();
        assertEquals(3, eventoFinal.getBoletosDisponibles(), "Debe descontar la cantidad comprada");

        List<Compra> compras = compraRepository.findAll().stream()
                .filter(c -> c.getEvento() != null && evento.getId().equals(c.getEvento().getId())).toList();
        assertEquals(1, compras.size());
        assertEquals(2, compras.get(0).getCantidad());
        assertEquals(2 * evento.getPrecio(), compras.get(0).getTotal(), 0.001);
    }

    // ---------- TC-12: compra rechazada por disponibilidad insuficiente ----------
    @Test
    void tc12CompraRechazadaPorDisponibilidadInsuficiente() {
        Usuario usuario = crearUsuario(correoUnico("tc12"), Rol.USUARIO);
        Evento evento = crearEvento("Evento TC-12 Sin Disponibilidad " + UUID.randomUUID(), 1);
        iniciarSesion(usuario.getEmail());

        driver.get(baseUrl() + "/eventos/" + evento.getId());
        WebElement cantidad = driver.findElement(By.id("cantidad"));
        cantidad.sendKeys("5");
        // El input tiene max=boletosDisponibles; se envia el formulario de compra por
        // JavaScript (no el de "Cerrar sesion" de la barra de navegacion, que tambien
        // esta presente en el DOM) para omitir la validacion nativa del navegador y
        // ejercer la validacion real del servidor en EventoServiceImpl.comprarBoletos().
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].closest('form').submit();", cantidad);
        pausaDeSincronizacion();

        assertTrue(driver.getCurrentUrl().contains("error=sin_disponibilidad"));
        assertTrue(driver.getPageSource().contains("No hay suficientes boletos disponibles"));

        Evento eventoFinal = eventoRepository.findById(evento.getId()).orElseThrow();
        assertEquals(1, eventoFinal.getBoletosDisponibles(), "No debe descontarse disponibilidad en una compra rechazada");
    }

    // ---------- Utilidades ----------

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String correoUnico(String prefijo) {
        return "selenium." + prefijo + "." + UUID.randomUUID() + "@ticketr.test";
    }

    private Usuario crearUsuario(String email, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Selenium " + email);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rol);
        usuario = usuarioRepository.save(usuario);
        usuariosCreados.add(usuario.getId());
        return usuario;
    }

    private Evento crearEvento(String nombre, int boletosDisponibles) {
        Evento evento = new Evento();
        evento.setNombre(nombre);
        evento.setDescripcion("Evento sembrado por CasosFuncionalesSeleniumTest.");
        evento.setLugar("Automatizacion");
        evento.setFecha(LocalDateTime.now().plusDays(10));
        evento.setCapacidad(boletosDisponibles);
        evento.setBoletosDisponibles(boletosDisponibles);
        evento.setPrecio(10000.0);
        evento = eventoRepository.save(evento);
        eventosCreados.add(evento.getId());
        return evento;
    }

    private void iniciarSesion(String email) {
        driver.get(baseUrl() + "/login");
        driver.findElement(By.id("username")).sendKeys(email);
        WebElement password = driver.findElement(By.id("password"));
        password.sendKeys(PASSWORD);
        clickSubmitDelFormularioDe(password);
    }

    private void setFechaHora(WebElement campo, String valorIso) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];", campo, valorIso);
    }

    /**
     * La barra de navegacion incluye un formulario de "Cerrar sesion" con su propio
     * boton submit, que aparece antes en el DOM que el formulario de cada pagina. Un
     * selector generico "form button[type='submit']" hace clic en ese boton de logout
     * por error en cualquier pagina donde el usuario ya tiene sesion iniciada. En su
     * lugar, se ubica el formulario que contiene el campo ya llenado y se hace clic
     * unicamente en el boton submit de ESE formulario.
     */
    private void clickSubmitDelFormularioDe(WebElement campo) {
        WebElement formulario = campo.findElement(By.xpath("ancestor::form"));
        formulario.findElement(By.cssSelector("button[type='submit']")).click();
        pausaDeSincronizacion();
    }

    /**
     * Pausa breve tras un submit o una navegacion que dispara una redireccion del
     * lado del servidor. Bajo carga (varias pruebas Selenium corriendo en cadena en
     * Chrome headless), el redirect a veces tarda una fraccion de segundo mas en
     * completarse de lo que el "page load" normal de Selenium reporta, causando
     * lecturas de currentUrl/pageSource tomadas antes de que la navegacion termine
     * realmente. Esta pausa elimina esa condicion de carrera.
     */
    private void pausaDeSincronizacion() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
