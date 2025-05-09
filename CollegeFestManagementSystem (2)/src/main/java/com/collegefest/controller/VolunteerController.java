package com.collegefest.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.collegefest.model.Task;
import com.collegefest.model.Attendance;
import com.collegefest.model.Event;
import com.collegefest.model.ParticipantEvent;
import com.collegefest.model.Volunteer;
import com.collegefest.repository.AttendanceRepository;
import com.collegefest.repository.EventRepository;
import com.collegefest.repository.ParticipantEventRepository;
import com.collegefest.repository.TaskRepository;
import com.collegefest.repository.VolunteerRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class VolunteerController {
    
    @Autowired
    private VolunteerRepository volunteerRepo;

    
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired 
    private ParticipantEventRepository participantEventRepo;

    @Autowired  // Add this
    private TaskRepository taskRepo;
    

    @GetMapping("/volunteer/login")
    public String loginPage() {
        return "Volunteer_login";
    }
    @GetMapping("/volunteer/events")
    public String showEvents(Model model) {
        // Only show APPROVED events
        List<Event> events = eventRepository.findByStatus("APPROVED");
        model.addAttribute("events", events);
        return "volunteer_events";
    }
    @GetMapping("/volunteer/dashboard/attendance")
    public String attendancePage(HttpSession session, Model model) {
        Volunteer volunteer = (Volunteer) session.getAttribute("loggedInVolunteer");
        if (volunteer == null) {
            return "redirect:/volunteer/login";
        }
        
        List<Attendance> attendanceRecords = attendanceRepo.findByVolunteer(volunteer);
        model.addAttribute("attendanceRecords", attendanceRecords);
        model.addAttribute("volunteer", volunteer);
        return "volunteer_attendance";
    }
    @PostMapping("/volunteer/check-in")
    public String checkIn(HttpSession session) {
        Volunteer volunteer = (Volunteer) session.getAttribute("loggedInVolunteer");
        if (volunteer != null) {
            Attendance attendance = new Attendance();
            attendance.setVolunteer(volunteer);
            attendance.setCheckInTime(LocalDateTime.now());
            attendance.setStatus("PRESENT");
            attendanceRepo.save(attendance);
        }
        return "redirect:/volunteer/dashboard/attendance";
    }
    @PostMapping("/volunteer/check-out")
    public String checkOut(HttpSession session) {
        Volunteer volunteer = (Volunteer) session.getAttribute("loggedInVolunteer");
        if (volunteer != null) {
            // Find today's check-in record that doesn't have a check-out time
            List<Attendance> records = attendanceRepo.findByVolunteerAndCheckOutTimeIsNull(volunteer);
            if (!records.isEmpty()) {
                Attendance attendance = records.get(0);
                attendance.setCheckOutTime(LocalDateTime.now());
                attendanceRepo.save(attendance);
            }
        }
        return "redirect:/volunteer/dashboard/attendance";
    }

    @PostMapping("/volunteer/login")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       Model model,
                       HttpSession session) {
        Volunteer volunteer = volunteerRepo.findByUsernameAndPassword(username, password);
        if (volunteer != null) {
            session.setAttribute("loggedInVolunteer", volunteer);
            return "redirect:/volunteer/dashboard";
        } else {
            model.addAttribute("error", "Invalid Credentials");
            return "Volunteer_login";
        }
    }
    
    @GetMapping("/volunteer/signup")
    public String signupPage() {
        return "Volunteer_signup";
    }
    
    @PostMapping("/volunteer/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String email,
                         @RequestParam(required = false) String phoneNumber,
                         @RequestParam(required = false) String department,
                         Model model) {
        if (volunteerRepo.findByUsername(username) != null) {
            model.addAttribute("error", "Username already exists!");
            return "Volunteer_signup";
        }
        
        Volunteer volunteer = new Volunteer();
        volunteer.setUsername(username);
        volunteer.setPassword(password);
        volunteer.setEmail(email);
        volunteer.setPhoneNumber(phoneNumber);
        volunteer.setDepartment(department);
        
        volunteerRepo.save(volunteer);
        model.addAttribute("success", "Account created! Please login.");
        return "Volunteer_login";
    }

    @GetMapping("/volunteer/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Volunteer volunteer = (Volunteer) session.getAttribute("loggedInVolunteer");
        if (volunteer == null) {
            return "redirect:/volunteer/login";
        }
        List<Task> allTasks = taskRepo.findAll();  // This fetches all tasks
        model.addAttribute("tasks", allTasks);
        // Only fetch APPROVED events
        List<Event> events = eventRepository.findByStatus("APPROVED");
        List<Attendance> attendanceRecords = attendanceRepo.findByVolunteer(volunteer);
        List<ParticipantEvent> participantEvents = participantEventRepo.findAll();

        model.addAttribute("volunteer", volunteer);
        model.addAttribute("attendanceRecords", attendanceRecords);
        model.addAttribute("participantEvents", participantEvents);
        model.addAttribute("events", events);

        return "volunteer_dashboard";
    }
    
    
    @PostMapping("/volunteer/mark-attendance")
    public String markAttendance(
        @RequestParam Long participantEventId,
        @RequestParam String status,
        HttpSession session) {
        
        Volunteer volunteer = (Volunteer) session.getAttribute("loggedInVolunteer");
        if (volunteer == null) {
            return "redirect:/volunteer/login";
        }
        
        ParticipantEvent participantEvent = participantEventRepo.findById(participantEventId).orElse(null);
        if (participantEvent != null) {
            participantEvent.setAttendanceStatus(status);
            participantEventRepo.save(participantEvent);
        }
        
        return "redirect:/volunteer/dashboard";
    }
    @GetMapping("/volunteer/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/volunteer/login?logout";
    }
}