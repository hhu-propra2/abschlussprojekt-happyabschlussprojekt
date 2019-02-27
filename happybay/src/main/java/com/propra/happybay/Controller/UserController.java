package com.propra.happybay.Controller;

import com.propra.happybay.Model.*;
import com.propra.happybay.Model.HelperClassesForViews.GeraetWithRentEvent;
import com.propra.happybay.Model.HelperClassesForViews.NotificationMitAnfragePerson;
import com.propra.happybay.Repository.*;
import com.propra.happybay.ReturnStatus;
import com.propra.happybay.Service.ProPayService;
import com.propra.happybay.Service.UserServices.GeraetService;
import com.propra.happybay.Service.UserServices.MailService;
import com.propra.happybay.Service.UserServices.NotificationService;
import com.propra.happybay.Service.UserServices.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Controller
@ControllerAdvice
@RequestMapping(value = {"/user"})
public class UserController {
    @Autowired
    PersonRepository personRepository;
    @Autowired
    GeraetRepository geraetRepository;
    @Autowired
    public PasswordEncoder encoder;
    @Autowired
    private ProPayService proPayService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MailService mailService;
    @Autowired
    private GeraetService geraetService;
    @Autowired
    private RentEventRepository rentEventRepository;
    @Autowired
    PersonService personService;
    @Autowired
    NotificationService notificationService;

    public UserController(ProPayService proPayService, AccountRepository accountRepository, GeraetService geraetService, MailService mailService, NotificationRepository notificationRepository, PersonService personService, RentEventRepository rentEventRepository, PersonRepository personRepository, GeraetRepository geraetRepository, NotificationService notificationService) {
        this.personRepository = personRepository;
        this.geraetRepository=geraetRepository;
        this.notificationService=notificationService;
        this.rentEventRepository=rentEventRepository;
        this.personService=personService;
        this.notificationRepository=notificationRepository;
        this.mailService=mailService;
        this.geraetService=geraetService;
        this.accountRepository=accountRepository;
        this.proPayService=proPayService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        String name = principal.getName();

        notificationService.updateAnzahl(name);
        Person person = personRepository.findByUsername(name).get();
        if (person.getFoto().getBild().length > 0) {
            person.setEncode(person.getFoto().encodeBild());
        }

        model.addAttribute("person", person);
        if (person.getRole().equals("ROLE_ADMIN")) {
            return "redirect://localhost:8080/admin";
        } else {
            return "user/profile";
        }
    }

    @GetMapping("/myThings")
    public String myThings(Model model, Principal principal) {
        String name = principal.getName();
        notificationService.updateAnzahl(name);
        Person person = personRepository.findByUsername(name).get();
        model.addAttribute("person", person);
        model.addAttribute("geraete", geraetService.getAllByBesitzerWithBilder(name));
        return "user/myThings";
    }

    @GetMapping("/rentThings")
    public String rentThings(Model model, Principal principal) {
        String mieterName = principal.getName();
        notificationService.updateAnzahl(mieterName);

        Person person = personRepository.findByUsername(mieterName).get();

        List<RentEvent> activeRentEvents = rentEventRepository.findAllByMieterAndReturnStatus(mieterName, ReturnStatus.ACTIVE);
        activeRentEvents.addAll(rentEventRepository.findAllByMieterAndReturnStatus(mieterName, ReturnStatus.DEADLINE_CLOSE));
        activeRentEvents.addAll(rentEventRepository.findAllByMieterAndReturnStatus(mieterName, ReturnStatus.DEADLINE_OVER));
        activeRentEvents.addAll(rentEventRepository.findAllByMieterAndReturnStatus(mieterName, ReturnStatus.KAPUTT));
        activeRentEvents.addAll(rentEventRepository.findAllByMieterAndReturnStatus(mieterName, ReturnStatus.WAITING_FOR_CONFIRMATION));

        List<GeraetWithRentEvent> activeGeraete = new ArrayList<>();
        personService.checksActiveOrInActiveRentEvent(activeRentEvents, activeGeraete);

        List<RentEvent> bookedRentEvents = rentEventRepository.findAllByMieterAndReturnStatus(mieterName, ReturnStatus.BOOKED);
        List<GeraetWithRentEvent> bookedGeraete = new ArrayList<>();

        personService.checksActiveOrInActiveRentEvent(bookedRentEvents, bookedGeraete);
        model.addAttribute("person", person);
        model.addAttribute("activeGeraete", activeGeraete);
        model.addAttribute("bookedGeraete", bookedGeraete);
        return "user/rentThings";
    }

    @GetMapping("/notifications")
    public String makeNotifications(Model model, Principal principal) {
        String name = principal.getName();
        notificationService.updateAnzahl(name);

        Person person = personRepository.findByUsername(name).get();
        model.addAttribute("person", person);
        List<Notification> notificationList = notificationService.findAllByBesitzer(name);
        NotificationMitAnfragePerson notificationMitAnfragePerson = new NotificationMitAnfragePerson();
        List<NotificationMitAnfragePerson> notificationMitAnfragePersonList = new ArrayList<>();
        for(int i=0; i < notificationService.findAllByBesitzer(name).size(); i++){
            notificationMitAnfragePerson.setAnfragePerson(personRepository.findByUsername(notificationList.get(i).getAnfragePerson()).get());
            notificationMitAnfragePerson.setNotification(notificationList.get(i));
            notificationMitAnfragePersonList.add(notificationMitAnfragePerson);
        }
        model.addAttribute("notifications",notificationMitAnfragePersonList);
        return "user/notifications";
    }

    @GetMapping("/anfragen/{id}")
    public String anfragen(@PathVariable Long id, Model model, Principal principal) {
        String name = principal.getName();
        Person person = personRepository.findByUsername(name).get();
        Geraet geraet = geraetRepository.findById(id).get();
        Account account = accountRepository.findByAccount(name).get();
        model.addAttribute("account",account);
        model.addAttribute("person", person);
        Geraet geraet1 = geraetRepository.findById(id).get();

        model.addAttribute("account", account);

        model.addAttribute("geraet", geraet1);
        model.addAttribute("geraet", geraet);
        model.addAttribute("notification", new Notification());
        return "user/anfragen";
    }

    @GetMapping("/geraet/changeToRent/{id}")
    public String changeToRent(@PathVariable Long id, Model model) {
        Geraet geraet = geraetRepository.findById(id).get();
        Person person = personRepository.findByUsername(geraet.getBesitzer()).get();
        model.addAttribute("person", person);
        model.addAttribute("geraet", geraet);
        return "user/changeToRent";
    }

    @PostMapping("/geraet/changeToRent/{id}")
    public String changeToRent(@PathVariable Long id, @ModelAttribute("geraet") Geraet geraet, @RequestParam(value = "files",required = false) MultipartFile[] files) throws IOException {
        RentEvent verfuegbar = new RentEvent();
        TimeInterval timeIntervalWithout = new TimeInterval(geraet.getMietezeitpunktStart(), geraet.getMietezeitpunktEnd());
        TimeInterval timeInterval = geraetService.convertToCET(timeIntervalWithout);
        verfuegbar.setTimeInterval(timeInterval);

        Geraet geraet1 = geraetRepository.findById(id).get();
        List<Bild> bilds = new ArrayList<>();
        personService.umwechsleMutifileZumBild(files, bilds);
        geraet1.setBilder(bilds);
        geraet1.setKosten(geraet.getKosten());
        geraet1.setTitel(geraet.getTitel());
        geraet1.setBeschreibung(geraet.getBeschreibung());
        geraet1.setKaution(geraet.getKaution());
        geraet1.setForsale(false);
        geraet1.setAbholort(geraet.getAbholort());
        geraetRepository.save(geraet1);
        geraet1.getVerfuegbareEvents().add(verfuegbar);
        geraetRepository.save(geraet1);
        return "redirect://localhost:8080/user/myThings";
    }

    @PostMapping("/anfragen/{id}")
    public String anfragen(@PathVariable Long id, @ModelAttribute(name = "notification") Notification notification,
                           Principal principal) throws Exception {

        Notification newNotification = new Notification();
        newNotification.setType("request");
        newNotification.setAnfragePerson(principal.getName());
        newNotification.setGeraetId(id);
        newNotification.setMessage(notification.getMessage());
        newNotification.setMietezeitpunktStart(new Date(notification.getMietezeitpunktStart().getTime() + 60 * 60 * 6000));
        newNotification.setMietezeitpunktEnd(new Date(notification.getMietezeitpunktEnd().getTime() + 60 * 60 * 6000));
        newNotification.setBesitzer(notification.getBesitzer());

        Geraet geraet = geraetRepository.findById(newNotification.getGeraetId()).get();
        Person person = personRepository.findByUsername(geraet.getBesitzer()).get();

        newNotification.setEncode(geraet.getEncode());
        notificationRepository.save(newNotification);

        mailService.sendAnfragMail(person, geraet, principal);

        return "redirect://localhost:8080";
    }

    @PostMapping("/sale/{id}")
    public String anfragen(@PathVariable Long id, Principal principal) throws Exception {
        Geraet geraet = geraetRepository.findById(id).get();
        Person person = personRepository.findByUsername(geraet.getBesitzer()).get();
        proPayService.ueberweisen(principal.getName(), person.getUsername(), geraet.getKosten());
        geraet.setBesitzer(principal.getName());
        geraetRepository.save(geraet);
        mailService.sendAnfragMail(person, geraet, principal);
        return "redirect://localhost:8080";
    }

    @GetMapping("/addGeraet")
    public String addGeraet(Model model, Principal principal) {
        String name = principal.getName();
        Person person = personRepository.findByUsername(name).get();

        model.addAttribute("person", person);
        return "user/addGeraet";
    }

    @PostMapping("/addGeraet")
    public String confirmGeraet(@ModelAttribute("geraet") Geraet geraet,
                                @RequestParam(name = "files",value = "files",required = false) MultipartFile[] files, Principal principal) throws IOException {
        List<Bild> bilds = new ArrayList<>();
        personService.umwechsleMutifileZumBild(files, bilds);
        RentEvent verfuegbar = new RentEvent();
        TimeInterval timeIntervalWithout = new TimeInterval(geraet.getMietezeitpunktStart(), geraet.getMietezeitpunktEnd());
        TimeInterval timeInterval = geraetService.convertToCET(timeIntervalWithout);
        if (!geraet.isForsale()) {
            verfuegbar.setTimeInterval(timeInterval);
            geraet.getVerfuegbareEvents().add(verfuegbar);
        }
        geraet.setBilder(bilds);
        geraet.setLikes(0);
        geraet.setBesitzer(principal.getName());

        geraetRepository.save(geraet);

        return "redirect://localhost:8080/user/myThings";
    }

    @GetMapping("/proPay")
    public String proPay(Model model, Principal principal) {
        String name = principal.getName();
        Person person = personRepository.findByUsername(name).get();
        model.addAttribute("person", person);
        try {
            proPayService.saveAccount(person.getUsername());
            Account account = accountRepository.findByAccount(person.getUsername()).get();
            List<Transaction> transactions = proPayService.getAllTransactionForPerson(name);
            model.addAttribute("transactions", transactions);
            model.addAttribute("account", account);
        }catch (Exception e){
            return"/user/propayNotAvailable";
        }
        return "user/proPay";
    }

    @PostMapping("/propayErhoehung")
    public String aufladenAntrag(@ModelAttribute("amount") int amount, @ModelAttribute("account") String account){
        try {
            proPayService.erhoeheAmount(account, amount);
        } catch (IOException e) {
            return"/user/propayNotAvailable";
        }
        return "redirect://localhost:8080";
    }

    @GetMapping("/geraet/{id}")
    public String geraet(@PathVariable Long id, Model model, Principal principal) {
        String person = principal.getName();
        Geraet geraet = geraetRepository.findById(id).get();
        Person personForComment = personRepository.findByUsername(geraet.getBesitzer()).get();
        List<String> encodes = geraetService.geraetBilder(geraet);
        model.addAttribute("encodes", encodes);
        model.addAttribute("person", personRepository.findByUsername(person).get());
        model.addAttribute("geraet", geraet);
        model.addAttribute("personForComment", personForComment);
        return "user/geraet";
    }

    @GetMapping("/BesitzerInfo/{id}")
    public String besitzerInfo(@PathVariable Long id, Model model) {
        Person besitzer = personRepository.findById(id).get();
        if (besitzer.getFoto().getBild().length > 0) {
            besitzer.setEncode(besitzer.getFoto().encodeBild());
        }
        model.addAttribute("person", besitzer);
        return "user/besitzerInfo";
    }
    @GetMapping("/mieterInfo/{id}")
    public String mieterInfo(@PathVariable Long id, Model model) {
        Person mieter = personRepository.findById(id).get();
        if (mieter.getFoto().getBild().length > 0) {
            mieter.setEncode(mieter.getFoto().encodeBild());
        }

        model.addAttribute("comments", mieter.getComments());
        model.addAttribute("person", mieter);
        return "user/mieterInfo";
    }

    @GetMapping("/geraet/edit/{id}")
    public String geraetEdit(@PathVariable Long id, Model model) {
        Person person = personRepository.findByUsername(geraetRepository.findById(id).get().getBesitzer()).get();

        Geraet geraet = geraetRepository.findById(id).get();
        model.addAttribute("person", person);
        model.addAttribute("geraet", geraet);
        return "user/edit";
    }

    @GetMapping("/geraet/zurueckgeben/{id}")
    public String geraetZurueck(@PathVariable Long id, Principal principal) throws Exception {
        RentEvent rentEvent = rentEventRepository.findById(id).get();
        rentEvent.setReturnStatus(ReturnStatus.WAITING_FOR_CONFIRMATION);
        rentEventRepository.save(rentEvent);

        Geraet geraet = geraetRepository.findById(rentEvent.getGeraetId()).get();

        Notification newNotification = new Notification();
        newNotification.setType("return");
        newNotification.setAnfragePerson(principal.getName());
        newNotification.setGeraetId(rentEventRepository.findById(id).get().getGeraetId());
        newNotification.setRentEventId(id);
        newNotification.setBesitzer(geraet.getBesitzer());
        notificationRepository.save(newNotification);

        Person person = personRepository.findByUsername(geraet.getBesitzer()).get();
        mailService.sendReturnMail(person, geraet);

        return "redirect://localhost:8080/user/rentThings";
    }

    @PostMapping("/geraet/delete/{id}")
    public String geraetDelete(@PathVariable Long id) {
        geraetRepository.deleteById(id);
        return "redirect://localhost:8080/user/myThings";
    }

    @PostMapping("/notification/refuseRequest/{id}")
    public String notificationRefuseRequest(@PathVariable Long id) throws Exception {
        Notification notification = notificationRepository.findById(id).get();
        String mieter = notification.getAnfragePerson();
        Geraet geraet = geraetRepository.findById(notification.getGeraetId()).get();

        Person person = personRepository.findByUsername(mieter).get();

        mailService.sendRefuseRequestMail(person, geraet);
        notificationRepository.deleteById(id);
        return "redirect://localhost:8080/user/notifications";
    }

    @PostMapping("/notification/acceptRequest/{id}")
    public String notificationAcceptRequest(@PathVariable Long id) throws Exception {

        Notification notification = notificationRepository.findById(id).get();
        String mieter = notification.getAnfragePerson();
        Geraet geraet = geraetRepository.findById(notification.getGeraetId()).get();
        int reservationId = 0;
        try {
            reservationId = proPayService.erzeugeReservation(mieter, geraet.getBesitzer(), geraet.getKaution());
        } catch (IOException e) {
            return "/user/propayNotAvailable";
        }

        TimeInterval timeInterval = new TimeInterval(notification.getMietezeitpunktStart(), notification.getMietezeitpunktEnd());
        RentEvent rentEvent = new RentEvent();
        rentEvent.setMieter(mieter);
        rentEvent.setTimeInterval(timeInterval);
        rentEvent.setGeraetId(geraet.getId());
        rentEvent.setReservationId(reservationId);
        rentEvent.setReturnStatus(ReturnStatus.BOOKED);
        geraet.getRentEvents().add(rentEvent);
        int index = personService.positionOfFreeBlock(geraet, rentEvent);
        personService.intervalZerlegen(geraet, index, rentEvent);
        geraetRepository.save(geraet);
        notificationRepository.deleteById(id);

        Person person = personRepository.findByUsername(mieter).get();
        mailService.sendAcceptRequestMail(person, geraet);

        return "redirect://localhost:8080/user/notifications";
    }

    @PostMapping("/notification/refuseReturn/{id}")
    public String notificationRefuseReturn(@PathVariable Long id, @ModelAttribute("grund") String grund) throws Exception {
        Notification notification = notificationRepository.findById(id).get();
        RentEvent rentEvent = rentEventRepository.findById(notification.getRentEventId()).get();
        rentEvent.setReturnStatus(ReturnStatus.KAPUTT);
        rentEvent.setGrundForReturn(grund);
        rentEventRepository.save(rentEvent);

        Person person = personRepository.findByUsername(rentEvent.getMieter()).get();
        Geraet geraet = geraetRepository.findById(rentEvent.getGeraetId()).get();
        mailService.sendRefuseReturnMail(person, geraet);
        personService.makeComment(geraet, person, grund);
        notificationRepository.deleteById(id);

        return "redirect://localhost:8080/user/notifications";
    }

    @PostMapping("/notification/acceptReturn/{id}")
    public String notificationAcceptReturn(@PathVariable Long id, @ModelAttribute("grund") String grund) throws Exception {
        Notification notification = notificationService.getNotificationById(id);
        RentEvent rentEvent = rentEventRepository.findById(notification.getRentEventId()).get();
        Geraet geraet = geraetRepository.findById(rentEvent.getGeraetId()).get();
        Person person = personRepository.findByUsername(rentEvent.getMieter()).get();
        mailService.sendAcceptReturnMail(person, geraet);
        personService.makeComment(geraet, person, grund);
//        geraetService.checkForTouchingIntervals(geraet, rentEvent);
        double amount = rentEvent.getTimeInterval().getDuration() * geraet.getKosten();
        try {
            proPayService.ueberweisen(notification.getAnfragePerson(), notification.getBesitzer(), (int) amount);
            proPayService.releaseReservation(rentEvent.getMieter(), rentEvent.getReservationId());
        }catch (IOException e){
            return"/user/propayNotAvailable";
        }
        geraet.getRentEvents().remove(rentEvent);
        geraetRepository.save(geraet);
        rentEventRepository.delete(rentEvent);
        notificationRepository.deleteById(id);
        return "redirect://localhost:8080/user/notifications";
    }

    @GetMapping("/PersonInfo/Profile/ChangeProfile")
    public String changeImg(Model model, Principal principal) {
        String name = principal.getName();
        Person person = personRepository.findByUsername(name).get();
        model.addAttribute("person", person);
        return "user/changeProfile";
    }

    @PostMapping("/PersonInfo/Profile/ChangeProfile")
    public String chageProfile(Model model, @RequestParam("file") MultipartFile file,
                               @ModelAttribute("person") Person p, Principal principal) throws IOException {
        model.addAttribute("person", personService.savePerson(principal, file, p));
        return "default/confirmationOfRegistration";
    }

    @GetMapping("/geraet/addLikes/{id}")
    public String like(@PathVariable Long id) {
        geraetService.addLike(id);
        return "redirect://localhost:8080";
    }

    @PostMapping("/geraet/edit/{id}")
    public String geraetEdit(Model model, @PathVariable Long id, @ModelAttribute Geraet geraet,
                             @RequestParam("files") MultipartFile[] files) throws IOException {
        geraetService.saveGeraet(files, geraet, id);
        List<Geraet> geraete = null;
        model.addAttribute("geraete", geraete);
        return "redirect://localhost:8080/user/myThings";
    }


    @ExceptionHandler(MultipartException.class)
    @ResponseBody
    String permittedSizeException (Exception e){
        e.printStackTrace();
        return "<h3>The file exceeds its maximum permitted size of 15 MB. Please reload your page.</h3>" +
                "    <div>\n" +
                "            <span>\n" +
                "                <a href=\"/\">Or if you want to back to home</a>\n" +
                "            </span>\n" +
                "    </div>";
    }
}
