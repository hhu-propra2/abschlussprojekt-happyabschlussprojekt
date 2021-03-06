package com.propra.happybay.Controller;

import com.propra.happybay.Model.*;
import com.propra.happybay.Repository.*;
import com.propra.happybay.Service.ProPayService;
import com.propra.happybay.Service.UserServices.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {TestContext.class, WebApplicationContext.class})
@WebAppConfiguration
public class UserControllerTest {
    private Person user = new Person();
    private Person admin = new Person();
    private Geraet geraet = new Geraet();
    private Account account = new Account();
    private RentEvent rentEvent=new RentEvent();
    private List<RentEvent> verfuegbareEvents=new ArrayList<>();
    List<Transaction> transactions=new ArrayList<>();
    Transaction transaction=new Transaction();
    Date start = new Date(2019,10,20);
    Date end = new Date(2019,11,21);
    private TimeInterval timeInterval = new TimeInterval(start,end);
    List<RentEvent> activeRentEvents=new ArrayList<>();

    private Bild bild = new Bild();
    private RentEvent verfuerbar = new RentEvent();
    @Autowired
    public PasswordEncoder encoder;
    private MockMvc mvc2;
    @Mock
    ProPayService proPayService;
    @Mock
    PersonRepository personRepository;
    @Mock
    GeraetRepository geraetRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    NotificationService notificationService;
    private Notification notification=new Notification();
    @Mock
    MailService mailService;
    @Mock
    PersonService personService;
    @Mock
    RentEventService rentEventService;
    @Mock
    GeraetService geraetService;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    RentEventRepository rentEventRepository;
    Principal principal = () -> "test";
    private final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("test.png");
    private final MockMultipartFile mockMultipartFile = new MockMultipartFile("test.png", "test.png", "image/png", inputStream);

    private MultipartFile[] multipartFiles = new MultipartFile[1];

    public UserControllerTest() throws IOException {
    }

    @Before
    public void setup() throws IOException {
        multipartFiles[0] = mockMultipartFile;
        byte[] bytes = new byte[20];
        new Random().nextBytes(bytes);
        bild.setBild(bytes);
        //User
        user.setUsername("test");
        user.setId(1L);
        user.setFoto(bild);
        user.setRole("ROLE_USER");
        user.setAdresse("test dusseldorf");
        user.setAnzahlNotifications(0);
        //Admin
        admin.setUsername("admin");
        admin.setId(2L);
        admin.setFoto(bild);
        admin.setRole("ROLE_ADMIN");
        personRepository.save(admin);
        rentEvent.setMieter(user);
        rentEvent.setTimeInterval(timeInterval);
        activeRentEvents.add(rentEvent);
        //Geraet
        geraet.setTitel("Das ist ein Test");
        geraet.setId(2L);
        geraet.setBesitzer(user);
        geraet.setKosten(3);
        geraet.setKaution(10);
        geraet.setMietezeitpunktEnd(end);
        geraet.setMietezeitpunktStart(start);
        geraet.setVerfuegbareEvents(verfuegbareEvents);
        //
        transactions.add(transaction);
        //Account
        account.setAccount(user.getUsername());
        account.setAmount(100.0);

        notification.setMietezeitpunktStart(start);
        notification.setMietezeitpunktEnd(end);
        geraet.getVerfuegbareEvents().add(verfuerbar);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/jsp/view/");
        viewResolver.setSuffix(".jsp");

        when(personService.findByPrincipal(principal)).thenReturn(user);
        when(personRepository.findByUsername(any())).thenReturn(Optional.ofNullable(user));
        when(personRepository.findById(any())).thenReturn(Optional.ofNullable(user));
        doNothing().when(personService).checksActiveOrInActiveRentEvent(any(),any());


        when(geraetRepository.findById(any())).thenReturn(Optional.ofNullable(geraet));
        mvc2 = MockMvcBuilders.standaloneSetup(new UserController(rentEventService,proPayService,accountRepository,geraetService,mailService,notificationRepository,personService,rentEventRepository, personRepository,geraetRepository,notificationService))
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void timeIntervalComputesDuration() {
        assertEquals(timeInterval.getDuration(), 31);
    }

    @Test
    public void proflieWithUser() throws Exception {
        mvc2.perform(get("/user/profile").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"));
    }
    @Test
    public void proflieWithAdmin() throws Exception {
        user.setRole("ROLE_ADMIN");
        mvc2.perform(get("/user/profile").principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect://localhost:8080/admin"));
    }

    @Test
    public void myThings() throws Exception {
        mvc2.perform(get("/user/myThings").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/myThings"));
    }

    @Test
    public void rentThings() throws Exception {

        when(rentEventService.getActiveEventsForPerson(user)).thenReturn(activeRentEvents);
        mvc2.perform(get("/user/rentThings").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/rentThings"));
        verify(rentEventRepository, Mockito.times(1)).findAllByMieterAndReturnStatus(any(),any());
    }

    @Test
    public void notifications() throws Exception {
        mvc2.perform(get("/user/notifications").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/notifications"));
    }

    @Test
    public void anfragenGet() throws Exception {

       when(geraetRepository.findById(2L)).thenReturn(Optional.ofNullable(geraet));
       when(accountRepository.findByAccount(user.getUsername())).thenReturn(Optional.ofNullable(account));

       mvc2.perform(get("/user/anfragen/{id}",2L).principal(principal))
                .andExpect(status().isOk())
               .andExpect(view().name("user/anfragen"));
    }
    @Test
    public void anfragenPost() throws Exception {

        when(geraetRepository.findById(2L)).thenReturn(Optional.ofNullable(geraet));
        doNothing().when(mailService).sendAnfragMail(any(),any(),any());
        mvc2.perform(post("/user/anfragen/{id}",2L).principal(principal)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .flashAttr("notification",notification))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect://localhost:8080"));
    }
    @Test
    public void proPay() throws Exception {
        when(accountRepository.findByAccount(anyString())).thenReturn(Optional.ofNullable(account));
        when(proPayService.getAllPastTransactionsForPerson(user)).thenReturn(transactions);
        mvc2.perform(get("/user/proPay").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/proPay"));

    }


    @Test
    public void besitzerInfo() throws Exception {
        mvc2.perform(get("/user/BesitzerInfo/{id}",1L).principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/besitzerInfo"));
    }
    @Test
    public void mieterInfo() throws Exception {
        mvc2.perform(get("/user/mieterInfo/{id}",1L))
                .andExpect(status().isOk())
                .andExpect(view().name("user/mieterInfo"));
    }


    @Test
    public void aufladenAntragOk() throws Exception {

        mvc2.perform(post("/user/propayErhoehung").param("amount","100").param("account","test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect://localhost:8080"));
    }
    @Test
    public void aufladenAntragWithErrorPropayService() throws Exception {
        doThrow(IOException.class).when(proPayService).erhoeheAmount("test",100);
        mvc2.perform(post("/user/propayErhoehung").param("amount","100").param("account","test"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/propayNotAvailable"));
    }
    @Test
    public void anfragen() throws Exception {

        mvc2.perform(post("/user/sale/{id}",1L).principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect://localhost:8080"));
    }

    @Test
    public void changeImg() throws Exception {

        mvc2.perform(get("/user/PersonInfo/Profile/ChangeProfile").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/changeProfile"));
    }
    @Test
    public void chageProfile() throws Exception {

        mvc2.perform(post("/user/PersonInfo/Profile/ChangeProfile").contentType(MediaType.APPLICATION_JSON).flashAttr("person",user).requestAttr("file",multipartFiles).principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("default/confirmationOfRegistration"));

    }


}