package com.example.telecom.audit;

import com.example.telecom.api.Times;
import com.example.telecom.model.AuditEvent;
import com.example.telecom.model.IdentityNumbers;
import com.example.telecom.model.StaffUser;
import com.example.telecom.repository.AuditEventRepository;
import com.example.telecom.security.StaffPrincipal;
import com.example.telecom.security.StaffRole;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Writes the audit trail. Events join the caller's transaction, so a change
 * that rolls back leaves no audit entry behind.
 */
@Service
public class AuditService {

    private static final int MAX_SUMMARY = 1000;
    private static final int MAX_USERNAME = 60;
    private static final Pattern ID_NUMBER = Pattern.compile("\\b[STFGM]\\d{7}[A-Z]\\b", Pattern.CASE_INSENSITIVE);

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** Records a successful action by the signed-in staff member. */
    public void record(AuditAction action, Object entityId, String summary) {
        record(Actor.current(), action, AuditEvent.Outcome.SUCCESS, entityId, summary);
    }

    public void record(Actor actor, AuditAction action, AuditEvent.Outcome outcome, Object entityId, String summary) {
        AuditEvent event = new AuditEvent();
        event.setOccurredAt(Times.now());
        event.setActorId(actor.id());
        event.setActorUsername(clean(actor.username(), MAX_USERNAME));
        event.setActorRole(actor.role() != null ? actor.role().name() : null);
        event.setAction(action.name());
        event.setEntityType(action.entity().name());
        event.setEntityId(entityId != null ? entityId.toString() : null);
        event.setSummary(clean(summary, MAX_SUMMARY));
        event.setOutcome(outcome);
        repository.save(event);
    }

    /**
     * Strips line breaks and masks anything shaped like an NRIC or FIN, so a
     * careless summary can never put a full ID number in the trail.
     */
    static String clean(String value, int maxLength) {
        String text = value == null || value.isBlank() ? "-" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        Matcher matcher = ID_NUMBER.matcher(text);
        StringBuilder masked = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(masked, Matcher.quoteReplacement(IdentityNumbers.mask(matcher.group())));
        }
        matcher.appendTail(masked);
        String result = masked.toString();
        return result.length() > maxLength ? result.substring(0, maxLength) : result;
    }

    /** Who performed an audited action. */
    public record Actor(Long id, String username, StaffRole role) {

        public static final Actor SYSTEM = new Actor(null, "system", null);

        public static Actor of(StaffUser user) {
            return new Actor(user.getId(), user.getUsername(), user.getRole());
        }

        /**
         * An actor that only has the username someone typed, such as a failed sign-in.
         */
        public static Actor unverified(String username) {
            return new Actor(null, username, null);
        }

        public static Actor current() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                return SYSTEM;
            }
            if (auth.getPrincipal() instanceof StaffPrincipal principal) {
                return new Actor(principal.id(), principal.username(), principal.role());
            }
            return new Actor(null, auth.getName(), null);
        }
    }
}
