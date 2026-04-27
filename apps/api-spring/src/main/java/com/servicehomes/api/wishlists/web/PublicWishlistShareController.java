package com.servicehomes.api.wishlists.web;

import com.servicehomes.api.wishlists.application.WishlistService;
import com.servicehomes.api.wishlists.application.dto.WishlistDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/wishlists/share")
@RequiredArgsConstructor
public class PublicWishlistShareController {

    private final WishlistService wishlistService;

    @GetMapping("/{token}")
    public ResponseEntity<WishlistDetailDto> getSharedWishlist(@PathVariable String token, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(wishlistService.getSharedWishlist(token, page, size));
    }
}
