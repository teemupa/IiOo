package com.aware.plugin.iioo;

        import android.content.Context;
        import android.database.Cursor;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.widget.TextView;
        import android.os.Handler;
        import android.os.Looper;
        import com.aware.utils.IContextCard;
        import java.text.SimpleDateFormat;


public class ContextCard implements IContextCard {

    private String gps_io = "-";
    private String tele_io = "-";
    private String mag_io = "-";
    private double updated;
    private String time;
    private String io_status;
    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private boolean isRegistered = false;
    TextView gps_status;
    TextView tele_status;
    TextView mag_status;
    TextView quess;
    TextView timestamp;
    private int refresh_interval = 1 * 1000;
    private Context context;
    private int counter = 0;

    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {
            counter++;
            if(gps_io.equals("-") || tele_io.equals("-") || mag_io.equals("-")){
                //TODO: "No data"
            }

            Cursor cur = context.getContentResolver().query(Provider.IODetector_Data.CONTENT_URI, null, null, null, Provider.IODetector_Data.TIMESTAMP + " DESC LIMIT 1");
            if (cur.getCount() > 0) {
                cur.moveToLast();
                io_status = cur.getString(cur.getColumnIndex(Provider.IODetector_Data.IO_STATUS));
                gps_io = cur.getString(cur.getColumnIndex(Provider.IODetector_Data.GPS_OP));
                tele_io = cur.getString(cur.getColumnIndex(Provider.IODetector_Data.TELE_OP));
                mag_io = cur.getString(cur.getColumnIndex(Provider.IODetector_Data.MAG_OP));
                updated = cur.getDouble(cur.getColumnIndex(Provider.IODetector_Data.TIMESTAMP));
            }
            cur.close();

            gps_status.setText(gps_io);
            tele_status.setText(tele_io);
            mag_status.setText(mag_io);
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss - dd.MM.yyyy");
            time = format.format(updated);
            timestamp.setText("Last update: " + time);
            quess.setText("So we think you are... " + io_status);

            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

    //private static ContextReceiver IO_receiver = new ContextReceiver();

    /*public static class ContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            gps_io = intent.getExtras().getString(Plugin.GPS_OP);
            tele_io = intent.getExtras().getString(Plugin.TELE_OP);
            mag_io = intent.getExtras().getString(Plugin.MAG_OP);
            updated = intent.getExtras().getString(Plugin.UPDATED);
        }
    }*/


    //Empty constructor used to instantiate this card
    public ContextCard(){};

    @Override
    public View getContextCard(Context context) {
        //Inflate and return your card's layout. See LayoutInflater documentation.
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View card = (View) inflater.inflate(R.layout.card, null);

        this.context = context;
        gps_status = (TextView) card.findViewById(R.id.textGPS);
        tele_status = (TextView) card.findViewById(R.id.textTELE);
        mag_status = (TextView) card.findViewById(R.id.textMAG);
        quess = (TextView) card.findViewById(R.id.quessText);
        timestamp = (TextView) card.findViewById(R.id.timestamp);

        uiRefresher.postDelayed(uiChanger, refresh_interval);

        return card;
    }
}