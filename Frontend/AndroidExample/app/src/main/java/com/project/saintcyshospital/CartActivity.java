package com.project.saintcyshospital;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

/**
 * Screen that shows the user's current pharmacy cart.
 * <p>
 * It lists all medicines stored in {@link CartStore}, shows the current
 * total price, lets the user remove items by tapping on a row, and has a
 * button to continue to {@link CheckoutActivity}.
 * @author quinn beckman
 */
public class CartActivity extends BaseActivity {

    /** ListView that displays one line of text for each cart item. */
    private ListView cartList;
    /** TextView showing the formatted cart total. */
    private TextView totalTxt;
    /** Button that takes the user to the checkout screen. */
    private Button checkoutBtn;

    /**
     * Backing data for {@link #cartList}. Each string is a human-readable
     * summary of a single {@link CartStore.CartItem}.
     */
    private final ArrayList<String> lines = new ArrayList<>();
    /** Adapter that connects {@link #lines} to the {@link #cartList} view. */
    private ArrayAdapter<String> adapter;

    /**
     * Initializes the cart screen: sets up the layout, bottom navigation,
     * ListView adapter, click listeners, and loads the current cart contents.
     *
     * @param savedInstanceState previous state if the activity is being
     *                           re-created, or {@code null} on first launch
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        cartList = findViewById(R.id.cart_list);
        totalTxt = findViewById(R.id.cart_total_txt);
        checkoutBtn = findViewById(R.id.checkout_btn);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines);
        cartList.setAdapter(adapter);

        // When the user taps an item, remove one unit of that medicine.
        cartList.setOnItemClickListener((parent, view, position, id) -> {
            CartStore.CartItem ci = CartStore.get().items().get(position);
            CartStore.get().removeOne(ci.medicine.id);
            Toast.makeText(this, "Removed one: " + ci.medicine.name, Toast.LENGTH_SHORT).show();
            refresh();
        });

        // Go to the checkout screen when the button is pressed.
        checkoutBtn.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CheckoutActivity.class))
        );
        refresh();
    }

    /**
     * Called when the activity comes to the foreground again.
     * <p>
     * We re-run {@link #refresh()} so the list and total stay in sync with
     * any changes that might have happened while this screen was not visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    /**
     * Reloads cart data from {@link CartStore} and updates the UI.
     * <ul>
     *     <li>Rebuilds the {@link #lines} list with the latest items.</li>
     *     <li>Notifies the adapter so the ListView is redrawn.</li>
     *     <li>Updates the total price text.</li>
     *     <li>Enables or disables the checkout button depending on whether
     *         the cart is empty.</li>
     * </ul>
     */
    private void refresh() {
        lines.clear();
        for (CartStore.CartItem ci : CartStore.get().items()) {
            String line = ci.medicine.name + " · " + ci.medicine.dosage +
                    " · Qty " + ci.qty + " · " +
                    String.format("$%.2f", (ci.medicine.priceCents * ci.qty) / 100.0);
            lines.add(line);
        }
        adapter.notifyDataSetChanged();
        totalTxt.setText(String.format("Total: $%.2f", CartStore.get().totalCents() / 100.0));
        checkoutBtn.setEnabled(CartStore.get().totalCents() > 0);
    }
}
