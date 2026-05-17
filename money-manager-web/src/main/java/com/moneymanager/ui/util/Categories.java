package com.moneymanager.ui.util;

import java.util.List;

/** Single source of truth for the predefined category list used across the app. */
public final class Categories {

    public static final List<String> ALL = List.of(
            "Food", "Bills", "Groceries", "Entertainment", "Travel", "Other"
    );

    private Categories() {}
}
