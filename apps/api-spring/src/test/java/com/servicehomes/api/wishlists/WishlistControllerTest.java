package com.servicehomes.api.wishlists;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.wishlists.application.WishlistService;
import com.servicehomes.api.wishlists.application.dto.WishlistDetailDto;
import com.servicehomes.api.wishlists.web.PublicWishlistShareController;
import com.servicehomes.api.wishlists.web.WishlistController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WishlistControllerTest {

    @Test
    void publicShareEndpointReadsWithoutAuthPrincipal() {
        WishlistService wishlistService = mock(WishlistService.class);
        WishlistDetailDto detail = new WishlistDetailDto(UUID.randomUUID(), UUID.randomUUID(), "Shared", null, null, true, false, 0, 0, Instant.now(), "token", List.of(), List.of(), 0, false);
        when(wishlistService.getSharedWishlist("token", 0, 50)).thenReturn(detail);

        ResponseEntity<WishlistDetailDto> response = new PublicWishlistShareController(wishlistService).getSharedWishlist("token", 0, 50);

        assertThat(response.getBody()).isEqualTo(detail);
        verify(wishlistService).getSharedWishlist("token", 0, 50);
    }

    @Test
    void authenticatedControllerDelegatesToCurrentUserForPrivateReads() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        WishlistService wishlistService = mock(WishlistService.class);
        WishlistController controller = new WishlistController(currentUserService, wishlistService);
        UUID userId = UUID.randomUUID();
        UUID wishlistId = UUID.randomUUID();
        when(currentUserService.requireUserId(null)).thenReturn(userId);

        controller.getWishlist(null, wishlistId, 0, 20);

        verify(wishlistService).getWishlistDetail(userId, wishlistId, 0, 20);
    }
}
