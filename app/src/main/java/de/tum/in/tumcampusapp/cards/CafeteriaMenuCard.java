package de.tum.in.tumcampusapp.cards;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.CafeteriaActivity;
import de.tum.in.tumcampusapp.auxiliary.CafeteriaPrices;
import de.tum.in.tumcampusapp.auxiliary.Const;
import de.tum.in.tumcampusapp.cards.generic.NotificationAwareCard;
import de.tum.in.tumcampusapp.models.cafeteria.CafeteriaMenu;

import static de.tum.in.tumcampusapp.fragments.CafeteriaDetailsSectionFragment.showMenu;
import static de.tum.in.tumcampusapp.managers.CardManager.CARD_CAFETERIA;

/**
 * Card that shows the cafeteria menu
 */
public class CafeteriaMenuCard extends NotificationAwareCard {
    private static final String CAFETERIA_DATE = "cafeteria_date";
    private static final Pattern COMPILE = Pattern.compile("\\([^\\)]+\\)");
    private static final Pattern PATTERN = Pattern.compile("[0-9]");
    private int mCafeteriaId;
    private String mCafeteriaName;
    private Date mDate;
    private String mDateStr;
    private List<CafeteriaMenu> mMenus;

    public CafeteriaMenuCard(Context context) {
        super(CARD_CAFETERIA, context, "card_cafeteria");
    }

    public static CardViewHolder inflateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void updateViewHolder(RecyclerView.ViewHolder viewHolder) {
        super.updateViewHolder(viewHolder);
        CardViewHolder cardsViewHolder = (CardViewHolder) viewHolder;
        List<View> addedViews = cardsViewHolder.getAddedViews();
        mCard = viewHolder.itemView;
        mLinearLayout = (LinearLayout) mCard.findViewById(R.id.card_view);
        mTitleView = (TextView) mCard.findViewById(R.id.card_title);
        mTitleView.setText(getTitle());

        // Show date
        TextView mDateView = (TextView) mCard.findViewById(R.id.card_date);
        mDateView.setVisibility(View.VISIBLE);
        mDateView.setText(SimpleDateFormat.getDateInstance().format(mDate));

        //Remove additional views
        for (View view : addedViews) {
            mLinearLayout.removeView(view);
        }

        // Show cafeteria menu
        cardsViewHolder.setAddedViews(showMenu(mLinearLayout, mCafeteriaId, mDateStr, false));
    }

    /**
     * Sets the information needed to build the card
     *
     * @param id      Cafeteria id
     * @param name    Cafeteria name
     * @param dateStr Date of the menu in yyyy-mm-dd format
     * @param date    Date of the menu
     * @param menus   List of cafeteria menus
     */
    public void setCardMenus(int id, String name, String dateStr, Date date, List<CafeteriaMenu> menus) {
        mCafeteriaId = id;
        mCafeteriaName = name;
        mDateStr = dateStr;
        mDate = date;
        mMenus = menus;
    }

    @Override
    public String getTitle() {
        return mCafeteriaName;
    }

    @Override
    public Intent getIntent() {
        Intent i = new Intent(mContext, CafeteriaActivity.class);
        i.putExtra(Const.CAFETERIA_ID, mCafeteriaId);
        return i;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void discard(Editor editor) {
        editor.putLong(CAFETERIA_DATE, mDate.getTime());
    }

    @Override
    protected boolean shouldShow(SharedPreferences prefs) {
        final long prevDate = prefs.getLong(CAFETERIA_DATE, 0);
        return prevDate < mDate.getTime();
    }

    @Override
    protected Notification fillNotification(NotificationCompat.Builder notificationBuilder) {
        Map<String, String> rolePrices = CafeteriaPrices.getRolePrices(mContext);

        NotificationCompat.WearableExtender morePageNotification = new NotificationCompat.WearableExtender();

        StringBuilder allContent = new StringBuilder();
        StringBuilder firstContent = new StringBuilder();
        for (CafeteriaMenu menu : mMenus) {
            if ("bei".equals(menu.typeShort)) {
                continue;
            }

            NotificationCompat.Builder pageNotification = new NotificationCompat.Builder(mContext, Const.NOTIFICATION_CHANNEL_DEFAULT).setContentTitle(PATTERN.matcher(menu.typeLong).replaceAll("").trim());

            StringBuilder content = new StringBuilder(menu.name);
            if (rolePrices.containsKey(menu.typeLong)) {
                content.append('\n').append(rolePrices.get(menu.typeLong)).append(" €");
            }

            String contentString = COMPILE.matcher(content.toString()).replaceAll("").trim();
            pageNotification.setContentText(contentString);
            if ("tg".equals(menu.typeShort)) {
                if (!allContent.toString().isEmpty()) {
                    allContent.append('\n');
                }
                allContent.append(contentString);
            }
            if (firstContent.toString().isEmpty()) {
                firstContent.append(COMPILE.matcher(menu.name).replaceAll("").trim()).append('…');
            } else {
                morePageNotification.addPage(pageNotification.build());
            }
        }

        notificationBuilder.setWhen(mDate.getTime());
        notificationBuilder.setContentText(firstContent);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(allContent));
        Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.wear_cafeteria);
        morePageNotification.setBackground(bm);
        return morePageNotification.extend(notificationBuilder).build();
    }

    @Override
    public RemoteViews getRemoteViews(Context context) {
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.cards_widget_card);
        remoteViews.setTextViewText(R.id.widgetCardTextView, this.getTitle());
        remoteViews.setImageViewResource(R.id.widgetCardImageView, R.drawable.ic_cutlery);
        return remoteViews;
    }
}
