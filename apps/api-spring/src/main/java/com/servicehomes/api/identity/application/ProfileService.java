package com.servicehomes.api.identity.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.identity.application.dto.AvatarUploadRequest;
import com.servicehomes.api.identity.application.dto.AvatarUploadResponse;
import com.servicehomes.api.identity.application.dto.HostProfileDto;
import com.servicehomes.api.identity.application.dto.MeDto;
import com.servicehomes.api.identity.application.dto.ProfileDto;
import com.servicehomes.api.identity.application.dto.UpdateProfileRequest;
import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.ProfileRepository;
import com.servicehomes.api.identity.domain.Role;
import com.servicehomes.api.identity.domain.RoleRepository;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.application.dto.ListingCardDto;
import com.servicehomes.api.listings.domain.ListingSearchRepository;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final ReservationRepository reservationRepository;
    private final ListingSearchRepository listingSearchRepository;
    private final EventPublisher eventPublisher;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    public MeDto getMe(UUID userId) {
        return toMeDto(requireUser(userId));
    }

    @Transactional
    public MeDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        Profile profile = ensureProfile(user);
        profile.setDisplayName(stripToNull(request.displayName()));
        profile.setBio(stripToNull(request.bio()));
        profile.setAvatarUrl(stripToNull(request.avatarUrl()));
        profile.setPhoneNumber(stripToNull(request.phoneNumber()));
        profile.setLocation(stripToNull(request.location()));
        profile.getLanguages().clear();
        profile.getLanguages().addAll(sanitizeLanguages(request.languages()));
        profileRepository.save(profile);

        eventPublisher.publish(
            "profile_updated",
            "user",
            user.getId().toString(),
            Map.of(
                "userId", user.getId().toString(),
                "hasAvatar", Boolean.toString(profile.getAvatarUrl() != null),
                "languagesCount", Integer.toString(profile.getLanguages().size())
            )
        );
        return toMeDto(user);
    }

    @Transactional
    public MeDto becomeHost(UUID userId) {
        User user = requireUser(userId);
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Verify your email before becoming a host");
        }
        if (hasRole(user, Role.RoleName.HOST)) {
            throw new IllegalArgumentException("You are already a host");
        }

        Role hostRole = roleRepository.findByName(Role.RoleName.HOST)
            .orElseThrow(() -> new IllegalStateException("HOST role not found"));
        user.getRoles().add(hostRole);
        userRepository.save(user);

        eventPublisher.publish(
            "became_host",
            "user",
            user.getId().toString(),
            Map.of(
                "userId", user.getId().toString(),
                "emailVerified", Boolean.toString(user.isEmailVerified())
            )
        );
        return toMeDto(user);
    }

    public HostProfileDto getHostProfile(UUID hostId) {
        User host = requireUser(hostId);
        if (!hasRole(host, Role.RoleName.HOST)) {
            throw new IllegalArgumentException("Host not found");
        }

        Profile profile = ensureProfile(host);
        List<ListingCardDto> listings = listingSearchRepository.findPublishedByHostId(hostId);
        long eligibleRequests = reservationRepository.countRequestsEligibleForResponseRate(hostId, Instant.now());
        long respondedWithin24h = reservationRepository.countRequestsRespondedWithin24Hours(hostId);
        Integer responseRate = eligibleRequests == 0
            ? null
            : (int) Math.round((respondedWithin24h * 100.0) / eligibleRequests);

        return new HostProfileDto(
            host.getId().toString(),
            displayNameFor(host),
            profile.getBio(),
            profile.getAvatarUrl(),
            profile.getLocation(),
            List.copyOf(profile.getLanguages()),
            host.getCreatedAt(),
            responseRate,
            listings.size(),
            listings
        );
    }

    public AvatarUploadResponse createAvatarUploadTarget(UUID userId, AvatarUploadRequest request) {
        requireUser(userId);
        if (!request.contentType().startsWith("image/")) {
            throw new IllegalArgumentException("Avatar uploads must be image files");
        }

        String safeFileName = request.fileName().replaceAll("[^a-zA-Z0-9._-]", "-");
        String s3Key = "profiles/" + userId + "/" + UUID.randomUUID() + "-" + safeFileName;

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .contentType(request.contentType())
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putRequest)
            .build();

        return new AvatarUploadResponse(
            s3Presigner.presignPutObject(presignRequest).url().toString(),
            buildPublicUrl(s3Key)
        );
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Profile ensureProfile(User user) {
        if (user.getProfile() == null) {
            Profile profile = Profile.builder()
                .user(user)
                .displayName(displayNameFor(user))
                .build();
            user.setProfile(profile);
        }
        return user.getProfile();
    }

    private MeDto toMeDto(User user) {
        return new MeDto(
            user.getId().toString(),
            user.getEmail(),
            user.isEmailVerified(),
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .collect(Collectors.toList()),
            toProfileDto(ensureProfile(user))
        );
    }

    private ProfileDto toProfileDto(Profile profile) {
        return new ProfileDto(
            profile.getFirstName(),
            profile.getLastName(),
            profile.getDisplayName(),
            profile.getBio(),
            profile.getAvatarUrl(),
            profile.getPhoneNumber(),
            profile.getLocation(),
            List.copyOf(profile.getLanguages()),
            profile.getCreatedAt()
        );
    }

    private boolean hasRole(User user, Role.RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private String displayNameFor(User user) {
        if (user.getProfile() != null && user.getProfile().getDisplayName() != null && !user.getProfile().getDisplayName().isBlank()) {
            return user.getProfile().getDisplayName();
        }
        return user.getEmail();
    }

    private List<String> sanitizeLanguages(List<String> languages) {
        if (languages == null) {
            return List.of();
        }
        return languages.stream()
            .map(this::stripToNull)
            .filter(language -> language != null)
            .distinct()
            .toList();
    }

    private String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildPublicUrl(String s3Key) {
        if (!endpoint.isBlank()) {
            return endpoint + "/" + bucket + "/" + s3Key;
        }
        return "https://" + bucket + ".s3." + System.getProperty("aws.region", "us-east-1") + ".amazonaws.com/" + s3Key;
    }
}
