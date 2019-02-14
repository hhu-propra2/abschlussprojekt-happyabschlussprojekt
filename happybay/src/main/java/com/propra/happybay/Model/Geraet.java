package com.propra.happybay.Model;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;
import java.util.List;

@Data
@Entity
public class Geraet {
    @Id
    @GeneratedValue
    Long id;
    String titel;
    String beschreibung;
    boolean verfuegbar;
    @OneToOne
    Person besitzer;
    int kosten;
    int kaution;
    String abholort;
    Date oeffdatum;
    @OneToMany(cascade = CascadeType.ALL)
    List<Bild> bilder;
}

