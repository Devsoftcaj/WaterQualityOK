package com.cmartosreyes.waterquality;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataActivity extends AppCompatActivity {

    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = null;
    private static final String TAG = "MY_APP_DEBUG_TAG";
    private StringBuilder recDataString = new StringBuilder();
    private static final int REQUEST_LOCATION = 1;
    String latitude, longitude, email;
    private double pH, temp, turbidity;
    String [] partes;


    LocationManager locationManager;

    Button mRecibirDatosButton, mEnviarDatosButton;
    TextView mTempTextView, mTurbidezTextView, mPHTextView, mLongitudTextView, mLatitudTextView;
    EditText mComentario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        setTitle("Recepción-Envío de datos");

        mTempTextView = (TextView) findViewById(R.id.tempTextView);
        mTurbidezTextView = (TextView) findViewById(R.id.turbidezTextView);
        mPHTextView = (TextView) findViewById(R.id.pHTextView);
        mRecibirDatosButton = (Button) findViewById(R.id.recibirDatosButton);
        mEnviarDatosButton = (Button) findViewById(R.id.enviarDatosbutton);
        mLatitudTextView = (TextView) findViewById(R.id.latitudTextView);
        mLongitudTextView = (TextView) findViewById(R.id.longitudTextView);
        mComentario = (EditText) findViewById(R.id.editTextComentario) ;

    /*    ActivityCompat.requestPermissions( this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
*/
        bluetoothIn = new Handler(Looper.getMainLooper()) {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {

                    //Interacción con los datos de ingreso

                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    //Log.d(TAG,readMessage);
                    int endOfLineIndex = recDataString.indexOf("~");
                    if(endOfLineIndex > 0){
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        if (recDataString.charAt(0)=='#'){
                            String data = recDataString.substring(1,recDataString.length()-1);
                            partes = data.split("\\+");
                            //String sensorTemp = recDataString.substring(1,recDataString.length());
                            mTempTextView.setText(partes[0]);
                            mTurbidezTextView.setText(partes[1]);
                            mPHTextView.setText(partes[2]);

                            locationManager = (LocationManager)  getSystemService(Context.LOCATION_SERVICE);
                            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                               OnGPS();
                            } else {
                               getLocation();
                            }

                        }
                        recDataString.delete(0,recDataString.length());
                    }


                   /* char readMessage = (char) msg.obj;
                    String mensaje = String.valueOf(readMessage);
                    mTempTextView.setText(mensaje);
                    */
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter

        mRecibirDatosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getBaseContext(), "Estableciendo conexión y recibiendo datos del dispositivo Arduino, espere...", Toast.LENGTH_LONG).show();

                MyConexionBT.write("a");
            }

        });

        mEnviarDatosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getBaseContext(), "Procediendo a la subida de datos, espere...", Toast.LENGTH_LONG).show();

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                Map<String, Object> measurement = new HashMap<>();
                measurement.put("email", email);
                temp = Double.parseDouble(partes[0]);
                turbidity = Double.parseDouble(partes[1]);
                pH = Double.parseDouble(partes[2]);
                measurement.put("Temperatura",temp);
                measurement.put("Turbidez",turbidity);
                measurement.put("pH",pH);
                measurement.put("Fecha",new Timestamp(System.currentTimeMillis()));
                measurement.put("Latitud", latitude);
                measurement.put("Longitud", longitude);
                measurement.put("Comentario", mComentario.getText().toString());

                db.collection("measurements")
                        .add(measurement)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                                Toast.makeText(getBaseContext(), "La subida de datos se ha completado con éxito.", Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                                Toast.makeText(getBaseContext(), "La subida de datos falló, intentelo de nuevo más tarde.", Toast.LENGTH_LONG).show();
                            }
                        });

            }
        });

    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Debe activar la ubicación GPS ¿Desea activarla?").setCancelable(false).setPositiveButton("Sí", new  DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                DataActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                DataActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            //locationManager = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null) {
                double lat = locationGPS.getLatitude();
                double longi = locationGPS.getLongitude();
                latitude = String.valueOf(lat);
                longitude = String.valueOf(longi);
                mLatitudTextView.setText(latitude);
                mLongitudTextView.setText(longitude);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 0, locListener);
                Toast.makeText(this, "La ubicación no está disponible, espere...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public LocationListener locListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.i(TAG, "Lat " + location.getLatitude() + " Long " + location.getLongitude());
        }

        public void onProviderDisabled(String provider) {
            Log.i(TAG, "onProviderDisabled()");
        }

        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled()");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged()");
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent = getIntent();
        address = intent.getStringExtra(BtActivity.EXTRA_DEVICE_ADDRESS);
        Bundle extra = intent.getExtras();
        email = extra.getString("email");
        Log.i(TAG, "EMAIL" +email);
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del socket de conexión bluetooth fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {}
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        { // Cuando se sale de la aplicación esta parte permite que no se deje abierto el socket
            btSocket.close();
        } catch (IOException e2) {}
    }

    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;
            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Envio de trama
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La conexión fallo", Toast.LENGTH_LONG).show();
                finish();
                //homeIntent();
            }
        }
    }

    private void homeIntent() {
        Intent loginIntent = new Intent(this, HomeActivity.class);
        startActivity(loginIntent);
    }
}