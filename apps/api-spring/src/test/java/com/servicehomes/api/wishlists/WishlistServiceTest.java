package com.servicehomes.api.wishlists;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.wishlists.application.WishlistService;
import com.servicehomes.api.wishlists.application.WishlistPhotoUploadService;
import com.servicehomes.api.wishlists.application.dto.*;
import com.servicehomes.api.wishlists.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock WishlistRepository wishlistRepository;
    @Mock WishlistItemRepository wishlistItemRepository;
    @Mock ListingRepository listingRepository;
    @Mock EventPublisher eventPublisher;
    @Mock WishlistPhotoUploadService wishlistPhotoUploadService;
    @InjectMocks WishlistService wishlistService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID wishlistId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    @Test
    void createsWishlistAndGeneratesShareToken() {
        when(wishlistRepository.save(any())).thenAnswer(invocation -> {
            Wishlist wishlist = invocation.getArgument(0);
            wishlist.setId(wishlistId);
            return wishlist;
        });
        Wishlist wishlist = wishlist();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));

        WishlistSummaryDto created = wishlistService.createWishlist(ownerId, new CreateWishlistRequest("Summer", null, false));
        ShareLinkDto share = wishlistService.generateShareLink(ownerId, wishlistId);

        assertThat(created.title()).isEqualTo("Summer");
        assertThat(share.token()).isNotBlank();
        assertThat(wishlist.isPublicList()).isTrue();
        verify(eventPublisher).publish(eq("wishlist_shared"), eq("wishlist"), eq(wishlistId.toString()), any());
    }

    @Test
    void createsPublicWishlistWithShareToken() {
        when(wishlistRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        wishlistService.createWishlist(ownerId, new CreateWishlistRequest("Public trip", null, true));

        verify(wishlistRepository).save(argThat(wishlist ->
            wishlist.isPublicList() && wishlist.getShareToken() != null && !wishlist.getShareToken().isBlank()
        ));
    }

    @Test
    void handlesDuplicateWishlistItemsIdempotently() {
        Wishlist wishlist = wishlist();
        Listing listing = Listing.builder().id(listingId).title("Loft").nightlyPrice(BigDecimal.TEN).photos(List.of()).build();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(wishlistItemRepository.findNextSortOrderByWishlistId(wishlistId)).thenReturn(0);
        when(wishlistItemRepository.saveAndFlush(any(WishlistItem.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));
        WishlistItem existing = WishlistItem.builder().id(UUID.randomUUID()).wishlist(wishlist).listing(listing).build();
        when(wishlistItemRepository.findByWishlistIdAndListingId(wishlistId, listingId)).thenReturn(Optional.of(existing));

        WishlistItemDto result = wishlistService.addItem(ownerId, wishlistId, new AddWishlistItemRequest(listingId, null, "search"));

        assertThat(result.listing().id()).isEqualTo(listingId);
    }

    @Test
    void rethrowsNonDuplicateWishlistItemFailures() {
        Wishlist wishlist = wishlist();
        Listing listing = Listing.builder().id(listingId).title("Loft").nightlyPrice(BigDecimal.TEN).photos(List.of()).build();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(wishlistItemRepository.findNextSortOrderByWishlistId(wishlistId)).thenReturn(0);
        var writeFailure = new org.springframework.dao.DataIntegrityViolationException("foreign key");
        when(wishlistItemRepository.saveAndFlush(any(WishlistItem.class))).thenThrow(writeFailure);
        when(wishlistItemRepository.findByWishlistIdAndListingId(wishlistId, listingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addItem(ownerId, wishlistId, new AddWishlistItemRequest(listingId, null, "search")))
            .isSameAs(writeFailure);
    }

    @Test
    void collaboratorsCanAddAndRemoveItems() {
        UUID collaboratorId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Wishlist wishlist = wishlist();
        wishlist.setCollaboratorIds(List.of(collaboratorId));
        Listing listing = Listing.builder().id(listingId).title("Loft").nightlyPrice(BigDecimal.TEN).photos(List.of()).build();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(wishlistItemRepository.findNextSortOrderByWishlistId(wishlistId)).thenReturn(0);
        when(wishlistItemRepository.saveAndFlush(any(WishlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        wishlistService.addItem(collaboratorId, wishlistId, new AddWishlistItemRequest(listingId, "yes", "wishlist"));

        when(wishlistItemRepository.findByWishlistIdAndId(wishlistId, itemId))
            .thenReturn(Optional.of(WishlistItem.builder().id(itemId).wishlist(wishlist).listing(listing).build()));
        wishlistService.removeItem(collaboratorId, wishlistId, itemId);
        verify(wishlistItemRepository).delete(any(WishlistItem.class));
    }

    @Test
    void collaboratorsCanRemoveListing() {
        UUID collaboratorId = UUID.randomUUID();
        Wishlist wishlist = wishlist();
        wishlist.setCollaboratorIds(List.of(collaboratorId));
        Listing listing = Listing.builder().id(listingId).title("Loft").nightlyPrice(BigDecimal.TEN).photos(List.of()).build();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdAndListingId(wishlistId, listingId))
            .thenReturn(Optional.of(WishlistItem.builder().id(UUID.randomUUID()).wishlist(wishlist).listing(listing).build()));

        wishlistService.removeListing(collaboratorId, wishlistId, listingId);
        verify(wishlistItemRepository).delete(any(WishlistItem.class));
    }

    @Test
    void listIncludesCollaborativeWishlists() {
        UUID collaboratorId = UUID.randomUUID();
        Wishlist shared = wishlist();
        shared.setId(UUID.randomUUID());
        shared.setOwnerId(ownerId);
        shared.setCollaboratorIds(List.of(collaboratorId));
        when(wishlistRepository.findAccessibleByUserId(collaboratorId)).thenReturn(List.of(shared));
        when(wishlistItemRepository.countByWishlistIds(List.of(shared.getId()))).thenReturn(List.of());

        List<WishlistSummaryDto> result = wishlistService.getWishlistsForUser(collaboratorId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).owner()).isFalse();
        verify(wishlistRepository).findAccessibleByUserId(collaboratorId);
        verify(wishlistRepository, never()).findByOwnerIdOrderByUpdatedAtDesc(any());
    }

    @Test
    void rejectsPartialOrDuplicateReorderPayloads() {
        Wishlist wishlist = wishlist();
        UUID firstItemId = UUID.randomUUID();
        UUID secondItemId = UUID.randomUUID();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdOrderBySortOrderAscAddedAtAsc(wishlistId)).thenReturn(List.of(
            WishlistItem.builder().id(firstItemId).wishlist(wishlist).sortOrder(0).build(),
            WishlistItem.builder().id(secondItemId).wishlist(wishlist).sortOrder(1).build()
        ));

        assertThatThrownBy(() -> wishlistService.reorderItems(ownerId, wishlistId, new ReorderWishlistItemsRequest(List.of(firstItemId, firstItemId))))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> wishlistService.reorderItems(ownerId, wishlistId, new ReorderWishlistItemsRequest(List.of(firstItemId))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesCollaboratorsBeforeSaving() {
        UUID collaboratorId = UUID.randomUUID();
        Wishlist wishlist = wishlist();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdOrderBySortOrderAscAddedAtAsc(eq(wishlistId), any())).thenReturn(org.springframework.data.domain.Page.empty());

        wishlistService.updateCollaborators(ownerId, wishlistId, new UpdateCollaboratorsRequest(List.of(collaboratorId, ownerId, collaboratorId)));

        assertThat(wishlist.getCollaboratorIds()).containsExactly(collaboratorId);
    }

    @Test
    void coverUploadReturnsPresignedUrlWithoutPersistingPublicUrl() {
        Wishlist wishlist = wishlist();
        String s3Key = "wishlists/%s/cover-123.jpg".formatted(wishlistId);
        WishlistCoverUploadResponse response = new WishlistCoverUploadResponse("https://upload", s3Key, "https://cdn/wishlists/key");
        WishlistCoverUploadRequest request = new WishlistCoverUploadRequest("image/jpeg", 100L);
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(wishlistPhotoUploadService.generateCoverUpload(wishlistId, request)).thenReturn(response);

        WishlistCoverUploadResponse result = wishlistService.createCoverUpload(ownerId, wishlistId, request);

        assertThat(result).isEqualTo(response);
        assertThat(wishlist.getCoverPhotoUrl()).isNull();
    }

    @Test
    void publicPrivacyGeneratesShareToken() {
        Wishlist wishlist = wishlist();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdOrderBySortOrderAscAddedAtAsc(eq(wishlistId), any())).thenReturn(org.springframework.data.domain.Page.empty());

        WishlistDetailDto result = wishlistService.updatePrivacy(ownerId, wishlistId, true);

        assertThat(wishlist.isPublicList()).isTrue();
        assertThat(wishlist.getShareToken()).isNotBlank();
        assertThat(result.shareToken()).isEqualTo(wishlist.getShareToken());
    }

    @Test
    void coverPhotoCanStillBeFinalizedExplicitly() {
        Wishlist wishlist = wishlist();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        String s3Key = "wishlists/%s/cover-123.jpg".formatted(wishlistId);
        when(wishlistPhotoUploadService.publicUrlForKey(s3Key)).thenReturn("https://cdn/wishlists/key");
        when(wishlistItemRepository.findByWishlistIdOrderBySortOrderAscAddedAtAsc(eq(wishlistId), any())).thenReturn(org.springframework.data.domain.Page.empty());

        WishlistDetailDto finalized = wishlistService.updateCoverPhoto(ownerId, wishlistId, new FinalizeWishlistCoverRequest(s3Key));

        assertThat(wishlist.getCoverPhotoUrl()).isEqualTo("https://cdn/wishlists/key");
        assertThat(finalized.coverPhotoUrl()).isEqualTo("https://cdn/wishlists/key");
    }

    @Test
    void publicShareDoesNotExposeInternalCollaboratorFields() {
        UUID collaboratorId = UUID.randomUUID();
        Wishlist wishlist = wishlist();
        wishlist.setPublicList(true);
        wishlist.setShareToken("share-token");
        wishlist.setCollaboratorIds(List.of(collaboratorId));
        when(wishlistRepository.findByShareToken("share-token")).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdOrderBySortOrderAscAddedAtAsc(eq(wishlistId), any())).thenReturn(org.springframework.data.domain.Page.empty());

        WishlistDetailDto shared = wishlistService.getSharedWishlist("share-token", 0, 50);

        assertThat(shared.ownerId()).isNull();
        assertThat(shared.shareToken()).isNull();
        assertThat(shared.collaboratorIds()).isEmpty();
        assertThat(shared.editable()).isFalse();
        assertThat(shared.owner()).isFalse();
    }

    @Test
    void finalizedCoverPhotoPersistsPublicUrl() {
        Wishlist wishlist = wishlist();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));
        String s3Key = "wishlists/%s/cover-123.jpg".formatted(wishlistId);
        when(wishlistPhotoUploadService.publicUrlForKey(s3Key)).thenReturn("https://cdn/wishlists/key");
        when(wishlistItemRepository.findByWishlistIdOrderBySortOrderAscAddedAtAsc(eq(wishlistId), any())).thenReturn(org.springframework.data.domain.Page.empty());

        WishlistDetailDto result = wishlistService.updateCoverPhoto(ownerId, wishlistId, new FinalizeWishlistCoverRequest(s3Key));

        assertThat(wishlist.getCoverPhotoUrl()).isEqualTo("https://cdn/wishlists/key");
        assertThat(result.coverPhotoUrl()).isEqualTo("https://cdn/wishlists/key");
    }

    @Test
    void rejectsCoverPhotoKeysOutsideWishlistPrefix() {
        Wishlist wishlist = wishlist();
        when(wishlistRepository.findById(wishlistId)).thenReturn(Optional.of(wishlist));

        assertThatThrownBy(() -> wishlistService.updateCoverPhoto(ownerId, wishlistId, new FinalizeWishlistCoverRequest("wishlists/other/cover-123.jpg")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Wishlist wishlist() {
        return Wishlist.builder()
            .id(wishlistId)
            .ownerId(ownerId)
            .title("Summer")
            .collaboratorIds(List.of())
            .items(List.of())
            .build();
    }
}
