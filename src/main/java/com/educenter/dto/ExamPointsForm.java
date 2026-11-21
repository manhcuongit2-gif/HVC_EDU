package com.educenter.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExamPointsForm {
    private Map<Long, BigDecimal> points = new LinkedHashMap<>();
    public Map<Long, BigDecimal> getPoints() { return points; }
    public void setPoints(Map<Long, BigDecimal> value) { points = value; }
}
