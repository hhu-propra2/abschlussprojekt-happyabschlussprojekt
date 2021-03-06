package com.propra.happybay.Service.UserServices;

import com.propra.happybay.Model.*;
import com.propra.happybay.Model.HelperClassesForViews.GeraetWithRentEvent;
import com.propra.happybay.Repository.GeraetRepository;
import com.propra.happybay.Repository.PersonRepository;
import com.propra.happybay.Repository.RentEventRepository;
import com.propra.happybay.Service.ProPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class PersonService {
    @Autowired
    PersonRepository personRepository;
    @Autowired
    GeraetRepository geraetRepository;
    @Autowired
    RentEventRepository rentEventRepository;
    @Autowired
    public PasswordEncoder encoder;
    @Autowired
    private ProPayService proPayService;

    public Person getByUsername(String username) {
        return personRepository.findByUsername(username).get();
    }

    public int positionOfFreeBlock(Geraet geraet, RentEvent anfrageRentEvent) {
        TimeInterval anfrageInterval = anfrageRentEvent.getTimeInterval();
        for (int i = 0; i < geraet.getVerfuegbareEvents().size(); i++) {
            TimeInterval verfuegbar = geraet.getVerfuegbareEvents().get(i).getTimeInterval();
            if ((verfuegbar.getStart().getTime() <= anfrageInterval.getStart().getTime()) && (verfuegbar.getEnd().getTime() >= anfrageInterval.getEnd().getTime())) {
                return i;
            }
        }
        return -1;
    }

    public void splitTimeIntervalsOfGeraetAvailability(Geraet geraet, int index, RentEvent rentEvent) {
        if (geraet.getVerfuegbareEvents().get(index).getTimeInterval().getStart().getTime()
                != rentEvent.getTimeInterval().getStart().getTime()) {
            Date dateBefore = new Date(rentEvent.getTimeInterval().getStart().getTime() - 24 * 3600 * 1000);
            TimeInterval timeInterval1 = new TimeInterval(geraet.getVerfuegbareEvents().get(index).getTimeInterval().getStart(),
                    dateBefore);
            RentEvent rentEvent1 = new RentEvent();
            rentEvent1.setTimeInterval(timeInterval1);
            geraet.getVerfuegbareEvents().add(rentEvent1);
        }
        if (rentEvent.getTimeInterval().getEnd().getTime()
                != geraet.getVerfuegbareEvents().get(index).getTimeInterval().getEnd().getTime()) {
            Date dateAfter = new Date(rentEvent.getTimeInterval().getEnd().getTime() + 24 * 3600 * 1000);
            TimeInterval timeInterval2 = new TimeInterval(dateAfter,
                    geraet.getVerfuegbareEvents().get(index).getTimeInterval().getEnd());
            RentEvent rentEvent2 = new RentEvent();
            rentEvent2.setTimeInterval(timeInterval2);
            geraet.getVerfuegbareEvents().add(rentEvent2);
        }
        geraet.getVerfuegbareEvents().remove(index);
        geraetRepository.save(geraet);
    }

    public void makeComment(Geraet geraet, Person person, String grund) {
        Comment comment = new Comment();
        comment.setDate(LocalDate.now());
        comment.setGeraetTitel(geraet.getTitel());
        comment.setMessage(grund);
        comment.setSenderFrom(geraet.getBesitzer().getUsername());
        comment.setPersonId(geraet.getBesitzer().getId());
        person.getComments().add(comment);
        personRepository.save(person);
    }

    public void makeAndSaveNewPerson(MultipartFile file, Person person) throws IOException {
        try {
            proPayService.saveAccount(person.getUsername());
        } catch (Exception e) {
            throw e;
        }
        Bild bild = new Bild();
        bild.setBild(file.getBytes());
        person.setFoto(bild);
        person.setRole("ROLE_USER");
        person.setPassword(encoder.encode(person.getPassword()));
        personRepository.save(person);
    }

    public void checksActiveOrInActiveRentEvent(List<RentEvent> RentEvents, List<GeraetWithRentEvent> geraete) {
        for (RentEvent rentEvent : RentEvents) {
            GeraetWithRentEvent geraetWithRentEvent = new GeraetWithRentEvent();
            geraetWithRentEvent.setGeraet(rentEvent.getGeraet());
            geraetWithRentEvent.setRentEvent(rentEvent);
            geraete.add(geraetWithRentEvent);
            if (geraetWithRentEvent.getGeraet().getBilder().get(0).getBild().length > 0) {
                geraetWithRentEvent.getGeraet().setEncode(geraetWithRentEvent.getGeraet().getBilder().get(0).encodeBild());
            }
        }
    }

    public void umwechsleMutifileZumBild(@RequestParam("files") MultipartFile[] files, List<Bild> bilds) throws IOException {
        for (MultipartFile file : files) {
            Bild bild = new Bild();
            bild.setBild(file.getBytes());
            bilds.add(bild);
        }
    }

    public Person savePerson(Principal principal, MultipartFile file, Person p) throws IOException {
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
        return person;
    }

    public Person findByPrincipal(Principal principal) {
        String name = principal.getName();
        Person person = personRepository.findByUsername(name).get();
        return person;
    }

    public void checkPhoto(Person person) {
        if (person.getFoto().getBild().length > 0) {
            person.setEncode(person.getFoto().encodeBild());
        }
    }
}
