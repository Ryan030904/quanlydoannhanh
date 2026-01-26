package com.pos.model;

public class CartItem {
    private final Item item;
    private int quantity;

    public CartItem(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getLineTotal() {
        return item.getPrice() * quantity;
    }
}
