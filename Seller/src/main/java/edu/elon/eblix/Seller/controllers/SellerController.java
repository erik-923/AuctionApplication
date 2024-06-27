package edu.elon.eblix.Seller.controllers;

import edu.elon.eblix.Seller.models.Item;
import edu.elon.eblix.Seller.services.SellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"itemforsale"})
public class SellerController {
    @Autowired
    SellerService sellerService;

    @PostMapping
    public Item listItem(@RequestBody Item item) {
        return this.sellerService.postItemForSale(item);
    }
}
