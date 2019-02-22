package com.propra.happybay.Controller;

import com.propra.happybay.Model.*;
import com.propra.happybay.Repository.*;
import com.propra.happybay.ReturnStatus;
import com.propra.happybay.Service.*;
import com.propra.happybay.Service.UserServices.MailService;
import com.propra.happybay.Service.UserServices.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = {"/user"})
public class UserController {
    @Autowired
    PersonRepository personRepository;
    @Autowired
    GeraetRepository geraetRepository;
    @Autowired
    public PasswordEncoder encoder;
    @Autowired
    private TransferRequestRepository transferRequestRepository;
    @Autowired
    private ProPayService proPayService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private GeraetMitReservationIDRepository geraetMitReservationIDRepository;
    @Autowired
    private MailService mailService;
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @Autowired
    private GeraetService geraetService;
    @Autowired
    private PersonService personService;
    @Autowired
    private NotificationService notificationService;

    private SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        String signedInPersonUsername = principal.getName();
        notificationService.updateAnzahl(signedInPersonUsername);
        Person person = personService.getByUsername(signedInPersonUsername);
        // WAS MACHT DAS?
        if (person.getFoto().getBild().length > 0) {
            person.setEncode(person.getFoto().encodeBild());
        }
        model.addAttribute("person", person);
        if (signedInPersonUsername.equals("admin")) {
            return "redirect:/admin/"; }
        else {
            return "user/profile";
        }
    }

    @GetMapping("/myThings")
    public String myThings(Model model, Principal principal) {
        String signedInPersonUsername = principal.getName();
        notificationService.updateAnzahl(signedInPersonUsername);
        Person person = personService.getByUsername(signedInPersonUsername);
        model.addAttribute("person", person);

        List<Geraet> geraete = geraetService.getAllByBesitzerWithBilder(signedInPersonUsername);
        model.addAttribute("geraete", geraete);
        return "user/myThings";
    }

    @GetMapping("/rentThings")
    public String rentThings(Model model, Principal principal) {
        String mieterName = principal.getName();
        notificationService.updateAnzahl(mieterName);
        Person person = personService.getByUsername(mieterName);
        model.addAttribute("person", person);

        List<Geraet> geraete = geraetService.getAllByBesitzerWithBilder(mieterName);
        model.addAttribute("geraete", geraete);
        return "user/rentThings";
    }

    @GetMapping("/notifications")
    public String makeNotifications(Model model, Principal principal) {
        String signedInPersonUsername = principal.getName();
        notificationService.updateAnzahl(signedInPersonUsername);
        Person person = personService.getByUsername(signedInPersonUsername);
        model.addAttribute("person", person);

        List<Notification> notifications = notificationService.findAllByBesitzer(signedInPersonUsername);
        model.addAttribute("notification", notifications);

        List<TransferRequest> transferRequestList = transferRequestRepository.findAll();
        model.addAttribute("transferRequestList",transferRequestList);
        return "user/notifications";
    }

    @GetMapping("/anfragen/{id}")
    public String anfragen(@PathVariable Long id, Model model, Principal principal) {
        String signedInPersonUsername = principal.getName();
        Person person = personService.getByUsername(signedInPersonUsername);
        model.addAttribute("person", person);

        Geraet geraet = geraetRepository.findById(id).get();
        model.addAttribute("geraet",geraet);
        return "user/anfragen";
    }

    @PostMapping("/anfragen/{id}")
    public String anfragen(@PathVariable Long id, @ModelAttribute Notification notification, Principal principal) throws Exception {
        notification.setType("request");
        notification.setGeraetId(id);

        Geraet geraet = geraetRepository.findById(notification.getGeraetId()).get();
        Person person = personRepository.findByUsername(geraet.getBesitzer()).get();

        notification.setEncode(geraet.getEncode());
        notificationRepository.save(notification);
        mailService.sendAnfragMail(person, geraet, principal);
        return "redirect:/";
    }

    @GetMapping("/addGeraet")
    public String addGeraet(Model model, Principal principal) {
        String signedInPersonUsername = principal.getName();
        Person person = personService.getByUsername(signedInPersonUsername);
        model.addAttribute("person", person);
        return "user/addGeraet";
    }

    @PostMapping("/addGeraet")
    public String confirmGeraet(@ModelAttribute("geraet") Geraet geraet,
                                @RequestParam("files") MultipartFile[] files, Principal principal) throws IOException {
        String signedInPersonUsername = principal.getName();
        geraetService.saveNewGeraet(files, signedInPersonUsername, geraet);
        personService.increaseAktionPunkte(signedInPersonUsername);
        return "redirect:/user/myThings";
    }

    @GetMapping("/proPay")
    public String proPay(Model model, Principal principal) {
        String signedInPersonUsername = principal.getName();
        notificationService.updateAnzahl(signedInPersonUsername);
        Person person = personService.getByUsername(signedInPersonUsername);
        proPayService.updateAccount(signedInPersonUsername);
        model.addAttribute("person", person);

        Account account = accountRepository.findByAccount(person.getUsername()).get();
        model.addAttribute("account", account);
        return "user/proPay";
    }

    @PostMapping("/propay")
    public String propay(Principal principal, @ModelAttribute("transferRequest") TransferRequest transferRequest){
        transferRequest.setUsername(principal.getName());
        transferRequestRepository.save(transferRequest);
        return "redirect:/";
    }

    @GetMapping("/geraet/{id}")
    public String geraet(@PathVariable Long id, Model model, Principal principal) {
        String signedInPersonUsername = principal.getName();
        Person person = personService.getByUsername(signedInPersonUsername);
        model.addAttribute("person", person);

        Geraet geraet = geraetRepository.findById(id).get();
        List<String> encodes = geraetService.geraetBilder(geraet);
        model.addAttribute("encodes", encodes);
        model.addAttribute("geraet", geraet);
        return "user/geraet";
    }
    @GetMapping("/BesitzerInfo/{id}")
    public String besitzerInfo(@PathVariable Long id, Model model){
        Geraet geraet = geraetRepository.findById(id).get();
        Person besitzer = personRepository.findByUsername(geraet.getBesitzer()).get();
        model.addAttribute("person", besitzer);
        return "user/besitzerInfo";
    }

    @GetMapping("/geraet/edit/{id}")
    public String geraetEdit(@PathVariable Long id, Model model) {
        Geraet geraet = geraetRepository.findById(id).get();
        String besitzer = geraet.getBesitzer();
        model.addAttribute("geraet", geraet);

        Person person = personService.getByUsername(besitzer);
        model.addAttribute("person", person);
        return "user/edit";
    }

    @GetMapping("/geraet/zurueckgeben/{id}")
    public String geraetZurueck(@PathVariable Long id, Principal principal) throws Exception {
        String signedInPersonUsername = principal.getName();
        Geraet geraet = geraetRepository.findById(id).get();
        geraet.setReturnStatus(ReturnStatus.WAITING);
        geraet.setZeitraum(-1);
        geraetRepository.save(geraet);

        Notification notification = notificationService.makeNotification(signedInPersonUsername,
                id, geraet);
        notification.setType("return");
        notificationRepository.save(notification);

        Person person = personRepository.findByUsername(geraet.getBesitzer()).get();
        mailService.sendReturnMail(person, geraet);

        return "redirect:/user/rentThings";
    }

    @PostMapping("/geraet/delete/{id}")
    public String geraetDelete(@PathVariable Long id) {
        geraetRepository.deleteById(id);
        return "redirect:/user/myThings";
    }

    @PostMapping("/notification/refuseRequest/{id}")
    public String notificationRefuseRequest(@PathVariable Long id) throws Exception {
        Notification notification = notificationRepository.findById(id).get();
        String mieter = notification.getAnfragePerson();
        Geraet geraet = geraetRepository.findById(notification.getGeraetId()).get();

        Person person = personRepository.findByUsername(mieter).get();

        mailService.sendRefuseRequestMail(person, geraet);
        notificationRepository.deleteById(id);
        return "redirect:/user/notifications";
    }
    @PostMapping("/notification/acceptRequest/{id}")
    public String notificationAcceptRequest(@PathVariable Long id, Principal principal) throws Exception {

        Notification notification = notificationRepository.findById(id).get();
        String mieter = notification.getAnfragePerson();
        Geraet geraet = geraetRepository.findById(notification.getGeraetId()).get();
        geraet.setVerfuegbar(false);
        geraet.setMieter(mieter);
        geraet.setZeitraum(notification.getZeitraum());
//        LocalDate endzeit = notification.getMietezeitPunkt().toLocalDate().plusDays(notification.getZeitraum());
//        geraet.setEndzeitpunkt(endzeit);
//        geraetRepository.save(geraet);

        RentEvent rentEvent = new RentEvent();
        rentEvent.setMieter(mieter);
        rentEvent.setTimeInterval(new TimeInterval(notification.getMietezeitpunktStart(), notification.getMietezeitpunktEnd()));
        geraet.getRentEvents().add(rentEvent);
        int index = userDetailsServiceImpl.positionOfFreeBlock(geraet, rentEvent);
        userDetailsServiceImpl.intervalZerlegen(geraet, index, rentEvent);
        geraetRepository.save(geraet);

        notificationRepository.deleteById(id);
        int reservationId = proPayService.erzeugeReservation(mieter, geraet.getBesitzer(), (int) geraet.getKaution());
        GeraetMitReservationID geraetMitReservationID = new GeraetMitReservationID();
        geraetMitReservationID.setGeraetID(geraet.getId());
        geraetMitReservationID.setReservationID(new Long(reservationId));
        geraetMitReservationIDRepository.save(geraetMitReservationID);

        Person person = personRepository.findByUsername(mieter).get();
        mailService.sendAcceptRequestMail(person, geraet);

        return "redirect:/user/notifications";
    }
    @PostMapping("/notification/refuseReturn/{id}")
    public String notificationRefuseReturn(@PathVariable Long id, @ModelAttribute("grund") String grund) throws Exception {
        Notification notification=notificationRepository.findById(id).get();
        Geraet geraet = geraetRepository.findById(notification.getGeraetId()).get();
        geraet.setGrundReturn(grund);
        geraet.setReturnStatus(ReturnStatus.KAPUTT);
        geraetRepository.save(geraet);
        Person person = personRepository.findByUsername(geraet.getMieter()).get();
        mailService.sendRefuseReturnMail(person, geraet);

        notificationRepository.deleteById(id);
        return "redirect:/user/notifications";
    }

    @PostMapping("/notification/acceptReturn/{id}")
    public String notificationAcceptReturn(@PathVariable Long id) throws Exception {
        Notification notification = notificationRepository.findById(id).get();

        Geraet geraet = geraetRepository.findById(notification.getGeraetId()).get();

        Person person = personRepository.findByUsername(geraet.getMieter()).get();
        mailService.sendAcceptReturnMail(person, geraet);

        geraet.setVerfuegbar(true);
        geraet.setReturnStatus(ReturnStatus.DEFAULT);
        geraet.setMieter(null);
        geraetRepository.save(geraet);
        double amount = 3;//eraet.getZeitraum()*geraet.getKosten();
        proPayService.ueberweisen(notification.getAnfragePerson(), notification.getBesitzer(), (int) amount);

        GeraetMitReservationID geraetMitReservationID = geraetMitReservationIDRepository.findByGeraetID(geraet.getId());
        proPayService.releaseReservation(notification.getAnfragePerson(), geraetMitReservationID.getReservationID());
        notificationRepository.deleteById(id);
        geraetMitReservationIDRepository.deleteByReservationID(geraetMitReservationID.getReservationID());
        return "redirect:/user/notifications";
    }

    @GetMapping("/PersonInfo/Profile/ChangeProfile")
    public String changeImg(Model model, Principal principal){
        String name = principal.getName();
        Person person = personRepository.findByUsername(name).get();
        model.addAttribute("person", person);
        return "user/changeProfile";
    }
    @PostMapping("/PersonInfo/Profile/ChangeProfile")
    public String chageProfile(Model model, @RequestParam("file") MultipartFile file,
                               @ModelAttribute("person") Person p, Principal principal) throws IOException {
        String name = principal.getName();
        Person person = personRepository.findByUsername(name).get();
        Bild bild = new Bild();
        bild.setBild(file.getBytes());
        person.setFoto(bild);
        person.setNachname(p.getNachname());
        person.setKontakt(p.getKontakt());
        person.setVorname(p.getVorname());
        person.setAdresse(p.getAdresse());
        personRepository.save(person);
        model.addAttribute("person", person);
        return "default/confirmationOfRegistration";
    }

    @PostMapping("/geraet/edit/{id}")
    public String geraetEdit(Model model, @PathVariable Long id, @ModelAttribute Geraet geraet,
                             @RequestParam("files") MultipartFile[] files) throws IOException {
        Geraet geraet1 = geraetRepository.findById(id).get();
        List<Bild> bilds = new ArrayList<>();
        for (MultipartFile file : files) {
            Bild bild = new Bild();
            bild.setBild(file.getBytes());
            bilds.add(bild);
        }
        geraet1.setBilder(bilds);
        geraet1.setKosten(geraet.getKosten());
        geraet1.setTitel(geraet.getTitel());
        geraet1.setBeschreibung(geraet.getBeschreibung());
        geraet1.setKaution(geraet.getKaution());
        geraet1.setAbholort(geraet.getAbholort());

        geraetRepository.save(geraet1);
        List<Geraet> geraete = null;//personRepository.findByUsername(person.getName()).get().getMyThings();
        model.addAttribute("geraete", geraete);
        return "redirect:/user/myThings";
    }

    @GetMapping("/bezahlen/{id}")
    public String bezahlen(Model model,@ModelAttribute Geraet geraet, Principal person, @PathVariable Long id) {
        Geraet geraet1 = geraetRepository.findById(id).get();
        String mieterName= person.getName();
        geraet1.setMieter(mieterName);
        geraetRepository.save(geraet1);
        return "user/confirmBezahlen";
    }

    @GetMapping("/aboutUs")
    public String about(Model model, Principal principal){
        if(principal != null){
            String name = principal.getName();
            if(personRepository.findByUsername(name).isPresent()) {
                model.addAttribute("person", personRepository.findByUsername(name).get());
            }
        }
        return "default/aboutUs";
    }

    @GetMapping("/geraet/addLikes/{id}")
    public String like(@PathVariable Long id) {
        Geraet geraet = geraetRepository.findById(id).get();
        geraet.setLikes(geraet.getLikes() + 1);
        geraetRepository.save(geraet);
        return "redirect:/";
    }

    public UserController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }
}
