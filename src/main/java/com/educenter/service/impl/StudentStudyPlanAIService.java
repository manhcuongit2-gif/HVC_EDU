package com.educenter.service.impl;

import com.educenter.service.AIService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentStudyPlanAIService {
    private final AIService ai;

    public StudentStudyPlanAIService(AIService ai) { this.ai = ai; }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generate(Long userId, String systemPrompt, String userPrompt) {
        return ai.generateText(userId, "ROLE_STUDENT", systemPrompt, userPrompt);
    }
}
