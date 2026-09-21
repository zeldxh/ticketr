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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automatiza TC-13 (G10_SC405_LN_TestCases.xlsx, suite "Compra de Boletos"),
 * el unico caso marcado como "Por automatizar" desde su diseno en el Avance 2
 * y confirmado como candidato de automatizacion en el Avance 3.
 *
 * Reproduce con dos navegadores reales (Selenium WebDriver) el mismo escenario
 * verificado manualmente en la Ejecucion (Avance 3, Fig. 21-22): dos usuarios
 * autenticados intentan comprar simultaneamente el ultimo boleto disponible
 * de un mismo evento. Verifica RF-11: la compra debe ser atomica y mutuamente
 * exclusiva, sin sobreventa.
 *
 * Requiere Google Chrome instalado y la aplicacion arrancable localmente
 * (MySQL disponible segun application.properties). No se ejecuta contra
 * datos de otras pruebas: crea y limpia sus propios usuarios/evento/compras.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Tc13CompraConcurrenteSeleniumTest {

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

    private WebDriver driverA;
    private WebDriver driverB;
    private Evento evento;
    private Usuario usuarioA;
    private Usuario usuarioB;

    @BeforeEach
    void setUp() {
        // Correos unicos por ejecucion (no fijos): si una corrida anterior fallara
        // antes de llegar a tearDown(), un correo fijo dejaria un usuario huerfano
        // en la base de datos y la siguiente corrida chocaria con "correo duplicado"
        // en lugar de ejecutar la prueba real. Este fue el origen de fallos
        // intermitentes observados al repetir la prueba varias veces seguidas.
        usuarioA = crearUsuario("selenium.tc13.a." + UUID.randomUUID() + "@ticketr.test");
        usuarioB = crearUsuario("selenium.tc13.b." + UUID.randomUUID() + "@ticketr.test");

        evento = new Evento();
        evento.setNombre("TC-13 Selenium - Ultimo Boleto");
        evento.setDescripcion("Evento sembrado por la prueba automatizada de TC-13.");
        evento.setLugar("Automatizacion");
        evento.setFecha(LocalDateTime.now().plusDays(7));
        evento.setCapacidad(1);
        evento.setBoletosDisponibles(1);
        evento.setPrecio(10000.0);
        evento = eventoRepository.save(evento);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1280,900",
                "--no-sandbox", "--disable-dev-shm-usage", "--disable-extensions",
                "--disable-background-networking", "--disable-notifications");
        driverA = new ChromeDriver(options);
        driverB = new ChromeDriver(options);
        driverA.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
        driverB.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));

        iniciarSesion(driverA, usuarioA.getEmail());
        iniciarSesion(driverB, usuarioB.getEmail());
    }

    @AfterEach
    void tearDown() {
        if (driverA != null) {
            driverA.quit();
        }
        if (driverB != null) {
            driverB.quit();
        }
        if (evento != null && evento.getId() != null) {
            compraRepository.findAll().stream()
                    .filter(c -> c.getEvento() != null && evento.getId().equals(c.getEvento().getId()))
                    .forEach(c -> compraRepository.deleteById(c.getId()));
            eventoRepository.deleteById(evento.getId());
        }
        if (usuarioA != null && usuarioA.getId() != null) {
            usuarioRepository.deleteById(usuarioA.getId());
        }
        if (usuarioB != null && usuarioB.getId() != null) {
            usuarioRepository.deleteById(usuarioB.getId());
        }
    }

    @Test
    void dosComprasSimultaneasSobreUnUnicoBoletoNoDebenOcasionarSobreventa() throws InterruptedException {
        String urlDetalle = "http://localhost:" + port + "/eventos/" + evento.getId();
        driverA.get(urlDetalle);
        driverB.get(urlDetalle);

        CountDownLatch salida = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> comprarUnBoleto(driverA, salida));
        pool.submit(() -> comprarUnBoleto(driverB, salida));

        boolean terminaron = salida.await(20, TimeUnit.SECONDS);
        pool.shutdownNow();
        assertTrue(terminaron, "Ambas solicitudes de compra debieron completarse dentro del tiempo de espera");

        Evento eventoFinal = eventoRepository.findById(evento.getId()).orElseThrow();
        List<Compra> comprasDelEvento = compraRepository.findAll().stream()
                .filter(c -> c.getEvento() != null && evento.getId().equals(c.getEvento().getId()))
                .toList();

        assertEquals(0, eventoFinal.getBoletosDisponibles(),
                "La disponibilidad final debe llegar exactamente a 0, nunca a un valor negativo (RF-11)");
        assertEquals(1, comprasDelEvento.size(),
                "Solo una de las dos compras concurrentes debe haberse registrado (RF-11, sin sobreventa)");

        boolean unaExitosaYUnaRechazada =
                (driverA.getCurrentUrl().contains("compra_exitosa") && driverB.getCurrentUrl().contains("error=sin_disponibilidad"))
                        || (driverB.getCurrentUrl().contains("compra_exitosa") && driverA.getCurrentUrl().contains("error=sin_disponibilidad"));
        assertTrue(unaExitosaYUnaRechazada,
                "Una sesion debe terminar en compra_exitosa y la otra en error=sin_disponibilidad");
    }

    private void comprarUnBoleto(WebDriver driver, CountDownLatch salida) {
        try {
            WebElement cantidad = driver.findElement(By.id("cantidad"));
            cantidad.clear();
            cantidad.sendKeys("1");
            clickSubmitDelFormularioDe(cantidad);
        } finally {
            salida.countDown();
        }
    }

    /**
     * La barra de navegacion incluye un formulario de "Cerrar sesion" con su propio
     * boton submit, que aparece antes en el DOM que el formulario de la pagina. Un
     * selector generico "form button[type='submit']" hace clic en ese boton de logout
     * por error. En su lugar, se ubica el formulario que contiene el campo ya llenado
     * y se hace clic unicamente en el boton submit de ESE formulario.
     */
    private void clickSubmitDelFormularioDe(WebElement campo) {
        WebElement formulario = campo.findElement(By.xpath("ancestor::form"));
        formulario.findElement(By.cssSelector("button[type='submit']")).click();
    }

    private void pausaDeSincronizacion() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Usuario crearUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Selenium " + email);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(PASSWORD));
        usuario.setRol(Rol.USUARIO);
        return usuarioRepository.save(usuario);
    }

    private void iniciarSesion(WebDriver driver, String email) {
        driver.get("http://localhost:" + port + "/login");
        driver.findElement(By.id("username")).sendKeys(email);
        WebElement password = driver.findElement(By.id("password"));
        password.sendKeys(PASSWORD);
        clickSubmitDelFormularioDe(password);
        pausaDeSincronizacion();
    }
}
