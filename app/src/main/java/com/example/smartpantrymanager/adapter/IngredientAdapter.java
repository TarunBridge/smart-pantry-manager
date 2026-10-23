package com.example.smartpantrymanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.model.Ingredient;
import com.example.smartpantrymanager.util.FormatUtils;
import com.example.smartpantrymanager.util.UnitConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom adapter binding pantry rows from SQLite onto item_ingredient.xml.
 * The ViewHolder pattern is used so that findViewById() runs once per row
 * instead of on every scroll.
 */
public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder> {

    /** Callbacks back to the Activity, which owns navigation and the database. */
    public interface OnIngredientListener {
        void onEdit(Ingredient ingredient);

        void onDelete(Ingredient ingredient);
    }

    private final List<Ingredient> items = new ArrayList<>();
    private final OnIngredientListener listener;
    private final boolean showExpiryAlerts;
    private final boolean imperialUnits;

    public IngredientAdapter(OnIngredientListener listener, boolean showExpiryAlerts, boolean imperialUnits) {
        this.listener = listener;
        this.showExpiryAlerts = showExpiryAlerts;
        this.imperialUnits = imperialUnits;
    }

    /** Replaces the whole data set and redraws the list. */
    public void setItems(List<Ingredient> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class IngredientViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvQuantity;
        private final TextView tvExpiry;
        private final ImageButton btnDelete;

        IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(final Ingredient ingredient) {
            tvName.setText(FormatUtils.capitalise(ingredient.getName()));
            tvQuantity.setText(UnitConverter.format(ingredient.getQuantity(), ingredient.getUnit(), imperialUnits));

            // Expiry badge: always show expired items, only show "use soon" when the
            // expiring-soon alerts setting is switched on.
            Integer days = ingredient.daysUntilExpiry();
            if (ingredient.isExpired()) {
                tvExpiry.setVisibility(View.VISIBLE);
                tvExpiry.setText(FormatUtils.daysLabel(days));
                tvExpiry.setBackgroundResource(R.drawable.bg_expired_badge);
                tvExpiry.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
            } else if (showExpiryAlerts && ingredient.isExpiringSoon()) {
                tvExpiry.setVisibility(View.VISIBLE);
                tvExpiry.setText(FormatUtils.daysLabel(days));
                tvExpiry.setBackgroundResource(R.drawable.bg_expiring_badge);
                tvExpiry.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
            } else {
                tvExpiry.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onEdit(ingredient);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onDelete(ingredient);
                }
            });
        }
    }
}
