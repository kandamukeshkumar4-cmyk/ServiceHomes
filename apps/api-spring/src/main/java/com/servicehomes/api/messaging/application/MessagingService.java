package com.servicehomes.api.messaging.application;

import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.messaging.application.dto.InboxThreadDto;
import com.servicehomes.api.messaging.application.dto.MessageDto;
import com.servicehomes.api.messaging.application.dto.MessageThreadDto;
import com.servicehomes.api.messaging.application.dto.SendMessageRequest;
import com.servicehomes.api.messaging.domain.Message;
import com.servicehomes.api.messaging.domain.MessageRepository;
import com.servicehomes.api.messaging.domain.MessageThread;
import com.servicehomes.api.messaging.domain.MessageThreadRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ReservationRepository reservationRepository;
    private final MessageThreadRepository messageThreadRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageEmailNotifier messageEmailNotifier;

    @Transactional(readOnly = true)
    public List<InboxThreadDto> getInbox(UUID userId) {
        List<MessageThread> threads = messageThreadRepository.findByGuestIdOrHostId(userId, userId);
        Map<UUID, User> users = loadUsers(counterpartIds(threads, userId));

        return threads.stream()
            .map(thread -> toInboxDto(thread, userId, users))
            .filter(dto -> dto.lastMessageAt() != null)
            .sorted(Comparator.comparing(InboxThreadDto::lastMessageAt).reversed())
            .toList();
    }

    @Transactional
    public MessageThreadDto getThread(UUID reservationId, UUID userId) {
        Reservation reservation = findAuthorizedReservation(reservationId, userId);
        return buildThreadDto(reservation, userId, true);
    }

    @Transactional
    public MessageThreadDto sendMessage(UUID senderId, UUID reservationId, SendMessageRequest request) {
        Reservation reservation = findAuthorizedReservation(reservationId, senderId);
        MessageThread thread = messageThreadRepository.findByReservationId(reservationId)
            .orElseGet(() -> createThread(reservation));

        Message savedMessage = messageRepository.saveAndFlush(Message.builder()
            .thread(thread)
            .senderId(senderId)
            .content(request.content().trim())
            .build());

        notifyRecipient(thread, savedMessage, senderId);
        return buildThreadDto(reservation, senderId, false);
    }

    @Transactional
    public int markRead(UUID threadId, UUID userId) {
        MessageThread thread = messageThreadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Message thread not found"));
        ensureParticipant(userId, thread.getGuestId(), thread.getHostId());
        return messageRepository.markIncomingMessagesRead(threadId, userId, Instant.now());
    }

    private MessageThreadDto buildThreadDto(Reservation reservation, UUID currentUserId, boolean markRead) {
        MessageThread thread = messageThreadRepository.findByReservationId(reservation.getId()).orElse(null);
        if (thread == null) {
            return emptyThreadDto(reservation, currentUserId);
        }

        if (markRead) {
            markRead(thread.getId(), currentUserId);
        }

        List<Message> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        Map<UUID, User> users = loadUsers(userIdsForThread(thread, messages));
        long unreadCount = messageRepository.countByThreadIdAndSenderIdNotAndReadAtIsNull(thread.getId(), currentUserId);
        return toThreadDto(thread, reservation, currentUserId, messages, users, unreadCount);
    }

    private MessageThread createThread(Reservation reservation) {
        return messageThreadRepository.save(MessageThread.builder()
            .reservation(reservation)
            .guestId(reservation.getGuestId())
            .hostId(reservation.getListing().getHostId())
            .build());
    }

    private void notifyRecipient(MessageThread thread, Message message, UUID senderId) {
        UUID recipientId = recipientId(thread, senderId);
        User recipient = findUser(recipientId, "Recipient not found");
        User sender = findUser(senderId, "Sender not found");

        messageEmailNotifier.notifyIfNeeded(new MessageEmailNotifier.NewMessageEmailCommand(
            thread.getId(),
            message.getId(),
            recipientId,
            recipient.getEmail(),
            displayName(recipient, "Guest"),
            displayName(sender, "Guest"),
            thread.getReservation().getListing().getTitle(),
            preview(message.getContent()),
            message.getCreatedAt()
        ));
    }

    private Reservation findAuthorizedReservation(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        ensureParticipant(userId, reservation.getGuestId(), reservation.getListing().getHostId());
        return reservation;
    }

    private void ensureParticipant(UUID userId, UUID guestId, UUID hostId) {
        if (!userId.equals(guestId) && !userId.equals(hostId)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private InboxThreadDto toInboxDto(MessageThread thread, UUID userId, Map<UUID, User> users) {
        Message lastMessage = messageRepository.findTopByThreadIdOrderByCreatedAtDesc(thread.getId()).orElse(null);
        UUID counterpartId = counterpartId(thread, userId);
        User counterpart = users.get(counterpartId);
        Reservation reservation = thread.getReservation();

        return new InboxThreadDto(
            thread.getId(),
            reservation.getId(),
            reservation.getListing().getId(),
            reservation.getListing().getTitle(),
            coverUrl(reservation.getListing()),
            counterpartId,
            displayName(counterpart, "Guest"),
            avatarUrl(counterpart),
            lastMessage != null ? preview(lastMessage.getContent()) : null,
            lastMessage != null ? lastMessage.getCreatedAt() : null,
            messageRepository.countByThreadIdAndSenderIdNotAndReadAtIsNull(thread.getId(), userId)
        );
    }

    private MessageThreadDto toThreadDto(
        MessageThread thread,
        Reservation reservation,
        UUID currentUserId,
        List<Message> messages,
        Map<UUID, User> users,
        long unreadCount
    ) {
        UUID counterpartId = counterpartId(thread, currentUserId);
        User counterpart = users.get(counterpartId);

        List<MessageDto> messageDtos = messages.stream()
            .map(message -> toMessageDto(message, currentUserId, users.get(message.getSenderId())))
            .toList();

        return new MessageThreadDto(
            thread.getId(),
            reservation.getId(),
            reservation.getListing().getId(),
            reservation.getListing().getTitle(),
            coverUrl(reservation.getListing()),
            thread.getGuestId(),
            thread.getHostId(),
            counterpartId,
            displayName(counterpart, "Guest"),
            avatarUrl(counterpart),
            unreadCount,
            messageDtos
        );
    }

    private MessageDto toMessageDto(Message message, UUID currentUserId, User sender) {
        return new MessageDto(
            message.getId(),
            message.getSenderId(),
            displayName(sender, "Guest"),
            avatarUrl(sender),
            message.getContent(),
            message.getCreatedAt(),
            message.getReadAt(),
            message.getSenderId().equals(currentUserId)
        );
    }

    private MessageThreadDto emptyThreadDto(Reservation reservation, UUID currentUserId) {
        UUID guestId = reservation.getGuestId();
        UUID hostId = reservation.getListing().getHostId();
        UUID counterpartId = currentUserId.equals(guestId) ? hostId : guestId;
        User counterpart = userRepository.findById(counterpartId).orElse(null);

        return new MessageThreadDto(
            null,
            reservation.getId(),
            reservation.getListing().getId(),
            reservation.getListing().getTitle(),
            coverUrl(reservation.getListing()),
            guestId,
            hostId,
            counterpartId,
            displayName(counterpart, "Guest"),
            avatarUrl(counterpart),
            0,
            List.of()
        );
    }

    private Map<UUID, User> loadUsers(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));
    }

    private Set<UUID> counterpartIds(List<MessageThread> threads, UUID userId) {
        Set<UUID> ids = new HashSet<>();
        for (MessageThread thread : threads) {
            ids.add(counterpartId(thread, userId));
        }
        return ids;
    }

    private Set<UUID> userIdsForThread(MessageThread thread, List<Message> messages) {
        Set<UUID> ids = new HashSet<>();
        ids.add(thread.getGuestId());
        ids.add(thread.getHostId());
        for (Message message : messages) {
            ids.add(message.getSenderId());
        }
        return ids;
    }

    private UUID counterpartId(MessageThread thread, UUID userId) {
        return userId.equals(thread.getGuestId()) ? thread.getHostId() : thread.getGuestId();
    }

    private UUID recipientId(MessageThread thread, UUID senderId) {
        return senderId.equals(thread.getGuestId()) ? thread.getHostId() : thread.getGuestId();
    }

    private User findUser(UUID userId, String notFoundMessage) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(notFoundMessage));
    }

    private String displayName(User user, String fallback) {
        if (user == null) {
            return fallback;
        }

        Profile profile = user.getProfile();
        if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }

        return user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : fallback;
    }

    private String avatarUrl(User user) {
        if (user == null || user.getProfile() == null) {
            return null;
        }
        return user.getProfile().getAvatarUrl();
    }

    private String coverUrl(Listing listing) {
        return listing.getPhotos().stream()
            .filter(ListingPhoto::isCover)
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(listing.getPhotos().isEmpty() ? null : listing.getPhotos().get(0).getUrl());
    }

    private String preview(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.length() <= 140) {
            return trimmed;
        }
        return trimmed.substring(0, 137) + "...";
    }
}
