package com.collegefest.controller;

import com.collegefest.model.Participant;
import com.collegefest.repository.ParticipantRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.collegefest.repository.EventRepository;
import com.collegefest.repository.ParticipantEventRepository;
import com.collegefest.model.Event;
import com.collegefest.model.ParticipantEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ParticipantAuthController {

    @Autowired
    private ParticipantRepository participantRepo;
    
    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private ParticipantEventRepository participantEventRepo;


    @GetMapping("/participant/login")
    public String loginPage() {
        return "participant_login";
    }

    @PostMapping("/participant/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Participant participant = participantRepo.findByUsernameAndPassword(username, password);
        if (participant != null) {
            session.setAttribute("loggedInParticipant", participant);
            return "redirect:/participant/events";
        } else {
            model.addAttribute("error", "Invalid credentials");
            return "participant_login";
        }
    }

    @GetMapping("/participant/signup")
    public String signupPage() {
        return "participant_signup";
    }

    @PostMapping("/participant/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String email,
                         Model model) {
        if (participantRepo.findByUsername(username) != null) {
            model.addAttribute("error", "Username already exists!");
            return "participant_signup";
        }

        Participant participant = new Participant();
        participant.setUsername(username);
        participant.setPassword(password);
        participant.setEmail(email);
        participantRepo.save(participant);

        model.addAttribute("success", "Account created! Please log in.");
        return "participant_login";
    }

    @GetMapping("/participant/events")
    public String eventPage(HttpSession session, Model model) {
        Participant participant = (Participant) session.getAttribute("loggedInParticipant");
        if (participant == null) {
            return "redirect:/participant/login";
        }

        // Only fetch APPROVED events
        List<Event> approvedEvents = eventRepo.findByStatus("APPROVED");
        model.addAttribute("participant", participant);
        model.addAttribute("events", approvedEvents);

        // Fetch registered events for this participant (only from approved events)
        List<ParticipantEvent> registeredEvents = participantEventRepo.findByParticipant(participant);
        Set<Long> registeredEventIds = registeredEvents.stream()
                .map(pe -> pe.getEvent().getId())
                .collect(Collectors.toSet());

        model.addAttribute("registeredEventIds", registeredEventIds);
        return "participant_events";
    }

    @PostMapping("/participant/registerEvent")
    public String registerEvent(@RequestParam Long eventId, HttpSession session) {
        Participant participant = (Participant) session.getAttribute("loggedInParticipant");
        if (participant == null) {
            return "redirect:/participant/login";
        }

        // Only allow registration for APPROVED events
        Event event = eventRepo.findByIdAndStatus(eventId, "APPROVED");
        if (event != null) {
            ParticipantEvent pe = new ParticipantEvent();
            pe.setParticipant(participant);
            pe.setEvent(event);
            participantEventRepo.save(pe);
        }

        return "redirect:/participant/events";
    }


    @GetMapping("/participant/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/participant/login?logout";
    }
    
}
