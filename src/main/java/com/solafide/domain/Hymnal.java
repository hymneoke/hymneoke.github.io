package com.solafide.domain;

import java.util.List;

import lombok.Data;

@Data
public class Hymnal {
    private Integer id;
    private String title;
    private String publisher;
    private Integer year;
    private List<Hymn> hymns;
}
