package com.solafide.domain;

import java.util.List;

import lombok.Data;

@Data
public class Hymn {
    private String id;
    private String title;
    private String lyricist;
    private List<Stanza> text;
    private List<Tune> tunes;
}
