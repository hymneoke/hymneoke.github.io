package com.solafide.domain;

import java.util.List;

import lombok.Data;

@Data
public class Stanza {
    private boolean refrain;
    private List<String> lines;
    private boolean repeatRefrain;
}
