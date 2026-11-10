package com.enterpriseops.config;

import com.enterpriseops.asset.Asset;
import com.enterpriseops.asset.AssetRepository;
import com.enterpriseops.monitoring.SystemMetric;
import com.enterpriseops.monitoring.SystemMetricRepository;
import com.enterpriseops.ticket.Priority;
import com.enterpriseops.ticket.SlaPolicyRepository;
import com.enterpriseops.ticket.Ticket;
import com.enterpriseops.ticket.TicketRepository;
import com.enterpriseops.ticket.TicketStatus;
import com.enterpriseops.user.RoleRepository;
import com.enterpriseops.user.User;
import com.enterpriseops.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates SYNTHETIC demo users, assets, tickets and metrics. Enabled only with SEED_DEMO=true; skipped if users already exist. */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String[] CATEGORIES = {"network", "hardware", "software", "access", "database"};
    private static final String[] TITLES = {"VPN keeps dropping", "Laptop will not turn on", "Outlook crashes on startup",
        "Locked out of account", "Database query very slow", "Printer not working", "Cannot install application", "Wi-Fi is slow"};

    private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder;
    private final AssetRepository assets; private final TicketRepository tickets; private final SystemMetricRepository metrics;
    private final SlaPolicyRepository sla; private final AppProperties props;

    public DemoDataSeeder(UserRepository users, RoleRepository roles, PasswordEncoder encoder, AssetRepository assets,
                          TicketRepository tickets, SystemMetricRepository metrics, SlaPolicyRepository sla, AppProperties props) {
        this.users = users; this.roles = roles; this.encoder = encoder; this.assets = assets; this.tickets = tickets;
        this.metrics = metrics; this.sla = sla; this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) { log.info("Demo seed skipped: users already exist"); return; }
        String pw = props.demoPassword();
        if (pw == null || pw.length() < 8) { log.warn("Demo seed skipped: set DEMO_PASSWORD (8+ chars)"); return; }

        User admin = user("admin@enterpriseops.local", "Demo Admin", "ADMIN", pw);
        User manager = user("manager@enterpriseops.local", "Demo Manager", "MANAGER", pw);
        User agent1 = user("agent1@enterpriseops.local", "Demo Agent One", "SUPPORT_AGENT", pw);
        User agent2 = user("agent2@enterpriseops.local", "Demo Agent Two", "SUPPORT_AGENT", pw);
        User employee = user("employee@enterpriseops.local", "Demo Employee", "EMPLOYEE", pw);

        List<Asset> seeded = List.of(
            asset("SRV-001", "Web Server 1", Asset.Type.SERVER, admin), asset("SRV-002", "App Server 1", Asset.Type.SERVER, admin),
            asset("DB-001", "Primary Database", Asset.Type.DATABASE, manager), asset("NET-001", "Core Router", Asset.Type.NETWORK, admin),
            asset("LAP-001", "Employee Laptop", Asset.Type.LAPTOP, employee));

        Random rnd = new Random(42);
        Instant now = Instant.now();
        User[] agents = {agent1, agent2};
        for (int i = 0; i < 40; i++) {
            Ticket t = new Ticket();
            t.setTitle(TITLES[rnd.nextInt(TITLES.length)] + " (demo)");
            t.setDescription("[SYNTHETIC DEMO DATA] Generated ticket number " + i + ".");
            t.setCategory(CATEGORIES[rnd.nextInt(CATEGORIES.length)]);
            Priority p = Priority.values()[rnd.nextInt(4)];
            t.setPriority(p);
            t.setReporter(employee);
            Instant created = now.minus(rnd.nextInt(60 * 24), ChronoUnit.HOURS).minus(rnd.nextInt(60), ChronoUnit.MINUTES);
            t.setCreatedAt(created);
            sla.findByPriority(p).ifPresent(s -> t.setDueAt(created.plus(s.getResolutionMinutes(), ChronoUnit.MINUTES)));
            if (rnd.nextInt(3) > 0) t.setAssignee(agents[rnd.nextInt(2)]);
            int roll = rnd.nextInt(10);
            if (roll < 5 && t.getAssignee() != null) {
                t.setStatus(roll < 4 ? TicketStatus.RESOLVED : TicketStatus.CLOSED);
                t.setResolvedAt(created.plus(30 + rnd.nextInt(60 * 30), ChronoUnit.MINUTES));
                if (t.getResolvedAt().isAfter(now)) t.setResolvedAt(now);
            } else if (roll < 8) {
                t.setStatus(TicketStatus.IN_PROGRESS);
            }
            tickets.save(t);
        }
        for (Asset a : seeded.subList(0, 4)) {                       // normal-looking synthetic history for charts
            for (int k = 40; k >= 1; k--) {
                SystemMetric m = new SystemMetric();
                m.setAssetId(a.getId());
                m.setCpuPercent(dec(40 + rnd.nextGaussian() * 6)); m.setRamPercent(dec(55 + rnd.nextGaussian() * 5));
                m.setDiskPercent(dec(60 + rnd.nextGaussian() * 3)); m.setNetworkMbps(dec(Math.max(0, 120 + rnd.nextGaussian() * 20)));
                m.setSimulated(true);
                m.setRecordedAt(now.minus(k * 5L, ChronoUnit.MINUTES));
                metrics.save(m);
            }
        }
        log.info("Demo data created (SYNTHETIC). Accounts: admin@, manager@, agent1@, agent2@, employee@ enterpriseops.local");
    }

    private BigDecimal dec(double v) { return BigDecimal.valueOf(Math.min(100, Math.max(0, v))).setScale(2, RoundingMode.HALF_UP); }

    private User user(String email, String name, String role, String password) {
        User u = new User();
        u.setEmail(email); u.setFullName(name); u.setPasswordHash(encoder.encode(password));
        u.setRole(roles.findByName(role).orElseThrow());
        return users.save(u);
    }

    private Asset asset(String tag, String name, Asset.Type type, User owner) {
        Asset a = new Asset();
        a.setAssetTag(tag); a.setName(name); a.setAssetType(type); a.setOwner(owner); a.setLocation("Demo site");
        return assets.save(a);
    }
}
