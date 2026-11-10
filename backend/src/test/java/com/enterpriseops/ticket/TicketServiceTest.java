package com.enterpriseops.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.enterpriseops.ai.AiClient;
import com.enterpriseops.ai.AiPredictionRepository;
import com.enterpriseops.asset.AssetRepository;
import com.enterpriseops.audit.AuditService;
import com.enterpriseops.common.BadRequestException;
import com.enterpriseops.notification.NotificationService;
import com.enterpriseops.security.AppUserDetails;
import com.enterpriseops.ticket.TicketDtos.CreateTicketRequest;
import com.enterpriseops.user.Role;
import com.enterpriseops.user.User;
import com.enterpriseops.user.UserRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class TicketServiceTest {
    TicketRepository tickets = mock(TicketRepository.class);
    TicketCommentRepository comments = mock(TicketCommentRepository.class);
    SlaPolicyRepository sla = mock(SlaPolicyRepository.class);
    UserRepository users = mock(UserRepository.class);
    AssetRepository assets = mock(AssetRepository.class);
    AiClient ai = mock(AiClient.class);
    AiPredictionRepository predictions = mock(AiPredictionRepository.class);
    NotificationService notifications = mock(NotificationService.class);
    AuditService audit = mock(AuditService.class);
    TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(tickets, comments, sla, users, assets, ai, predictions, notifications, audit);
    }

    private static User user(long id, String roleName) throws Exception {
        Role role = new Role();
        set(role, "name", roleName);
        User u = new User();
        u.setFullName("User " + id); u.setEmail("u" + id + "@x.io"); u.setPasswordHash("h"); u.setRole(role);
        set(u, "id", id);
        return u;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void createFallsBackToOtherWhenAiIsDown() throws Exception {
        User employee = user(1, "EMPLOYEE");
        when(users.getReferenceById(1L)).thenReturn(employee);
        when(ai.classify(any())).thenReturn(Optional.empty());
        when(sla.findByPriority(any())).thenReturn(Optional.empty());
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.create(new CreateTicketRequest("VPN down", "Cannot connect", null, null, null), new AppUserDetails(employee));

        assertEquals("other", res.category());
        assertEquals(Priority.MEDIUM, res.priority());
        verify(predictions, never()).save(any());
    }

    @Test
    void createUsesAiCategoryAndStoresPrediction() throws Exception {
        User employee = user(1, "EMPLOYEE");
        when(users.getReferenceById(1L)).thenReturn(employee);
        when(ai.classify(any())).thenReturn(Optional.of(new AiClient.Classification("network", 0.93, "v1")));
        when(sla.findByPriority(any())).thenReturn(Optional.empty());
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.create(new CreateTicketRequest("VPN down", "Cannot connect", "", Priority.HIGH, null), new AppUserDetails(employee));

        assertEquals("network", res.category());
        verify(predictions).save(any());
    }

    @Test
    void invalidStatusTransitionIsRejected() throws Exception {
        Ticket t = new Ticket();
        t.setReporter(user(1, "EMPLOYEE"));
        when(tickets.findById(5L)).thenReturn(Optional.of(t));   // status defaults to OPEN
        assertThrows(BadRequestException.class, () -> service.changeStatus(5L, TicketStatus.CLOSED));
    }

    @Test
    void resolvingSetsResolvedAt() throws Exception {
        Ticket t = new Ticket();
        t.setReporter(user(1, "EMPLOYEE"));
        when(tickets.findById(5L)).thenReturn(Optional.of(t));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        service.changeStatus(5L, TicketStatus.RESOLVED);
        assertNotNull(t.getResolvedAt());
        verify(notifications).notifyUser(any(), eq("TICKET_RESOLVED"), any(), any());
    }

    @Test
    void employeeCannotReadSomeoneElsesTicket() throws Exception {
        User owner = user(1, "EMPLOYEE");
        User other = user(2, "EMPLOYEE");
        Ticket t = new Ticket();
        t.setReporter(owner);
        when(tickets.findById(9L)).thenReturn(Optional.of(t));
        assertThrows(AccessDeniedException.class, () -> service.get(9L, new AppUserDetails(other)));
    }

    @Test
    void cannotAssignTicketToEmployee() throws Exception {
        Ticket t = new Ticket();
        t.setReporter(user(1, "EMPLOYEE"));
        when(tickets.findById(5L)).thenReturn(Optional.of(t));
        when(users.findById(2L)).thenReturn(Optional.of(user(2, "EMPLOYEE")));
        assertThrows(BadRequestException.class, () -> service.assign(5L, 2L));
    }
}
