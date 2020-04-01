package com.peterlaurence.trekme;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.peterlaurence.trekme.core.TrekMeContext;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.mapsource.IGNCredentials;
import com.peterlaurence.trekme.core.mapsource.MapSource;
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle;
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials;
import com.peterlaurence.trekme.model.map.MapModel;
import com.peterlaurence.trekme.service.event.MapDownloadEvent;
import com.peterlaurence.trekme.service.event.Status;
import com.peterlaurence.trekme.ui.LocationProviderHolder;
import com.peterlaurence.trekme.ui.events.DrawerClosedEvent;
import com.peterlaurence.trekme.ui.mapcalibration.MapCalibrationFragment;
import com.peterlaurence.trekme.ui.mapcreate.MapCreateFragment;
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSelectedEvent;
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSettingsEvent;
import com.peterlaurence.trekme.ui.mapcreate.views.GoogleMapWmtsViewFragment;
import com.peterlaurence.trekme.ui.mapcreate.views.ign.IgnCredentialsFragment;
import com.peterlaurence.trekme.ui.mapimport.MapImportFragment;
import com.peterlaurence.trekme.ui.maplist.MapListFragment;
import com.peterlaurence.trekme.ui.maplist.MapSettingsFragment;
import com.peterlaurence.trekme.ui.maplist.events.ZipCloseEvent;
import com.peterlaurence.trekme.ui.maplist.events.ZipFinishedEvent;
import com.peterlaurence.trekme.ui.maplist.events.ZipProgressEvent;
import com.peterlaurence.trekme.ui.mapview.MapViewFragment;
import com.peterlaurence.trekme.ui.mapview.components.markermanage.MarkerManageFragment;
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment;
import com.peterlaurence.trekme.ui.record.RecordFragment;
import com.peterlaurence.trekme.ui.settings.SettingsFragment;
import com.peterlaurence.trekme.ui.trackview.TrackViewFragment;
import com.peterlaurence.trekme.viewmodel.LocationServiceViewModel;
import com.peterlaurence.trekme.viewmodel.MainActivityViewModel;
import com.peterlaurence.trekme.viewmodel.ShowMapListEvent;
import com.peterlaurence.trekme.viewmodel.ShowMapViewEvent;
import com.peterlaurence.trekme.viewmodel.common.LocationProvider;
import com.peterlaurence.trekme.viewmodel.common.LocationProviderFactory;
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.os.Build.VERSION_CODES.Q;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MapListFragment.OnMapListFragmentInteractionListener,
        MapImportFragment.OnMapArchiveFragmentInteractionListener,
        MapViewFragment.RequestManageTracksListener,
        MapSettingsFragment.MapCalibrationRequestListener,
        MarkerManageFragment.MarkerManageFragmentInteractionListener,
        MapViewFragment.RequestManageMarkerListener,
        LocationProviderHolder {

    private static final String MAP_FRAGMENT_TAG = "mapFragment";
    private static final String MAP_LIST_FRAGMENT_TAG = "mapListFragment";
    private static final String MAP_SETTINGS_FRAGMENT_TAG = "mapSettingsFragment";
    private static final String MAP_CALIBRATION_FRAGMENT_TAG = "mapCalibrationFragment";
    private static final String MAP_IMPORT_FRAGMENT_TAG = "mapImportFragment";
    private static final String MAP_CREATE_FRAGMENT_TAG = "mapCreateFragment";
    private static final String IGN_CREDENTIALS_FRAGMENT_TAG = "ignCredentialsFragment";
    private static final String WMTS_VIEW_FRAGMENT_TAG = "wmtsViewFragment";
    private static final String RECORD_FRAGMENT_TAG = "gpxFragment";
    private static final String TRACK_VIEW_FRAGMENT_TAG = "trackViewFragment";
    private static final String SETTINGS_FRAGMENT = "settingsFragment";
    private static final String TRACKS_MANAGE_FRAGMENT_TAG = "tracksManageFragment";
    private static final String MARKER_MANAGE_FRAGMENT_TAG = "markerManageFragment";
    private static final List<String> FRAGMENT_TAGS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add(MAP_FRAGMENT_TAG);
                add(MAP_LIST_FRAGMENT_TAG);
                add(MAP_SETTINGS_FRAGMENT_TAG);
                add(MAP_CALIBRATION_FRAGMENT_TAG);
                add(MAP_IMPORT_FRAGMENT_TAG);
                add(MAP_CREATE_FRAGMENT_TAG);
                add(IGN_CREDENTIALS_FRAGMENT_TAG);
                add(WMTS_VIEW_FRAGMENT_TAG);
                add(TRACKS_MANAGE_FRAGMENT_TAG);
                add(MARKER_MANAGE_FRAGMENT_TAG);
                add(RECORD_FRAGMENT_TAG);
                add(TRACK_VIEW_FRAGMENT_TAG);
                add(SETTINGS_FRAGMENT);
            }});
    /* Permission-group codes */
    private static final int REQUEST_MINIMAL = 1;
    private static final int REQUEST_MAP_CREATION = 2;

    private static final String[] PERMISSIONS_BELOW_ANDROID_10 = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String[] PERMISSIONS_ANDROID_10_AND_ABOVE = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private static final String[] PERMISSIONS_MAP_CREATION = {
            Manifest.permission.INTERNET
    };
    private static final String TAG = "MainActivity";
    private static final String KEY_BUNDLE_BACK = "keyBackFragmentTag";
    private String mBackFragmentTag;
    private FragmentManager fragmentManager;
    private Snackbar mSnackBarExit;
    private NavigationView mNavigationView;
    private MainActivityViewModel viewModel;
    private LocationProviderFactory locationProviderFactory;

    /* Used for notifications */
    private Notification.Builder builder;
    private NotificationManager notifyMgr;

    static {
        /* Setup default eventbus to use an index instead of reflection, which is recommended for
         * Android for best performance.
         * See http://greenrobot.org/eventbus/documentation/subscriber-index
         */
        try {
            EventBus.builder().addIndex(new MyEventBusIndex()).installDefaultEventBus();
        } catch (EventBusException e) {
            // don't care
        }
    }

    /**
     * Checks whether the app has permission to access fine location and to write to device storage.
     * If the app does not have the requested permissions then the user will be prompted.
     */
    public static void requestMinimalPermissions(Activity activity) {
        int permissionLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionLocation != PackageManager.PERMISSION_GRANTED || permissionWrite != PackageManager.PERMISSION_GRANTED) {
            // We don't have the required permissions, so we prompt the user
            if (android.os.Build.VERSION.SDK_INT < Q) {
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_BELOW_ANDROID_10,
                        REQUEST_MINIMAL
                );
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_ANDROID_10_AND_ABOVE,
                        REQUEST_MINIMAL
                );
            }
        }
    }

    private static boolean checkStoragePermissions(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT < Q) {
            int permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return (permissionWrite == PackageManager.PERMISSION_GRANTED);
        }
        return true; // we don't need write permission for Android >= 10
    }

    public boolean checkMapCreationPermission() {
        return hasPermissions(this, PERMISSIONS_MAP_CREATION);
    }

    /**
     * Request all the required permissions for map creation.
     */
    private void requestMapCreationPermission() {
        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_MAP_CREATION,
                REQUEST_MAP_CREATION
        );
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determine if we have an internet connection.
     */
    private boolean checkInternet() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private void warnIfNotInternet() {
        if (!checkInternet()) {
            showMessageInSnackbar(getString(R.string.no_internet));
        }
    }

    private void warnIfBadStorageState() {
        /* If something is wrong.. */
        if (!TrekMeContext.INSTANCE.checkAppDir()) {
            String warningTitle = getString(R.string.warning_title);
            if (TrekMeContext.INSTANCE.isAppDirReadOnly()) {
                /* If its read only for sure, be explicit */
                showWarningDialog(getString(R.string.storage_read_only), warningTitle, null);
            } else {
                /* Else, just say there is something wrong */
                showWarningDialog(getString(R.string.bad_storage_status), warningTitle, null);
            }
        }
    }

    private void warnNoStoragePerm() {
        showWarningDialog(getString(R.string.no_storage_perm), getString(R.string.warning_title),
                dialog -> {
                    if (!checkStoragePermissions(this)) {
                        finish();
                    }
                });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        MapListViewModel mapListViewModel = new ViewModelProvider(this).get(MapListViewModel.class);
        mapListViewModel.getZipEvents().observe(this, event -> {
            if (event instanceof ZipProgressEvent) {
                onZipProgressEvent((ZipProgressEvent) event);
            }
            if (event instanceof ZipFinishedEvent) {
                onZipFinishedEvent((ZipFinishedEvent) event);
            }
            if (event instanceof ZipCloseEvent) {
                // When resumed, the fragment is notified with this event (this is how LiveData
                // works). To avoid emitting a new notification for a ZipFinishedEvent, we use
                // ZipCloseEvent on which we do nothing.
            }
        });

        fragmentManager = this.getSupportFragmentManager();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                EventBus.getDefault().post(new DrawerClosedEvent());
            }
        };
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
        }
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            mNavigationView.setNavigationItemSelectedListener(this);

            View headerView = mNavigationView.getHeaderView(0);
            TextView versionTextView = headerView.findViewById(R.id.app_version);

            try {
                String version = "v." + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                versionTextView.setText(version);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (drawer != null) {
            mSnackBarExit = Snackbar.make(drawer, R.string.confirm_exit, Snackbar.LENGTH_SHORT);
        }

        initViewModels();
    }

    /**
     * The first time we create a retained fragment, we don't want it to be added to the back stack,
     * because if the user uses the back button, the fragment manager will "forget" it and there is
     * no way to retrieve with `FragmentManager.findFragmentByTag(string)`. <br>
     * Even worse, that last method is used to decide whether a retained fragment should be created
     * or not. So a "forgotten" retained fragment can be created several times. <br>
     * Consequently, the first time a retained fragment is created and shown, it is not added to the
     * back stack. In that particular case, to keep the back functionality, a {@code mBackFragmentTag}
     * is used to store the tag of the fragment to go back to.
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mBackFragmentTag != null) {
            switch (mBackFragmentTag) {
                case MAP_LIST_FRAGMENT_TAG:
                    showMapListFragment();
                    /* Clear the tag so that the back stack is popped the next time, unless a new retained
                     * fragment is created in the meanwhile. */
                    mBackFragmentTag = null;
                    break;
                case MAP_FRAGMENT_TAG:
                    showMapViewFragment();
                    break;
                case MAP_SETTINGS_FRAGMENT_TAG:
                    Map map = MapModel.INSTANCE.getSettingsMap();
                    if (map != null) {
                        showMapSettingsFragment(map.getId());
                        break;
                    } else {
                        mBackFragmentTag = null;
                    }
                case TRACKS_MANAGE_FRAGMENT_TAG:
                    showMapViewFragment();
                    break;
                case MARKER_MANAGE_FRAGMENT_TAG:
                    showMapViewFragment();
                    break;
                case IGN_CREDENTIALS_FRAGMENT_TAG:
                    showMapCreateFragment();
                    mBackFragmentTag = MAP_LIST_FRAGMENT_TAG;
                    break;
                default:
                    showMapListFragment();
                    mBackFragmentTag = null;
            }

        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            /* BACK button twice to exit */
            if (mSnackBarExit == null || mSnackBarExit.isShown()) {
                super.onBackPressed();
            } else {
                mSnackBarExit.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Show the app title */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        return true;
    }

    /**
     * Here should be all observable subscriptions on various ViewModel's LivaData.
     */
    private void initViewModels() {
        LocationServiceViewModel locationServiceViewModel = new ViewModelProvider(this).get(LocationServiceViewModel.class);
        locationServiceViewModel.getStatus().observe(this,
                isActive -> {
                    // Set the visibility of the "Track Statistics" fragment menu, which we want to
                    // see only if the LocationService is started.
                    setTrackStatsMenuVisibility(isActive);
                    // Then, invalidate
                    supportInvalidateOptionsMenu();
                }
        );
    }

    @Override
    public void onStart() {
        requestMinimalPermissions(this);

        /* This must be done before activity's onStart */
        if (checkStoragePermissions(this)) {
            TrekMeContext.INSTANCE.init(this);
        }

        super.onStart();

        /* Register event-bus */
        EventBus.getDefault().register(this);

        /* Start the view-model */
        viewModel.onActivityStart();
    }

    @Subscribe
    public void onShowMapListEvent(ShowMapListEvent event) {
        showMapListFragment();
        warnIfBadStorageState();
        mBackFragmentTag = null;
    }

    @Subscribe
    public void onShowMapViewEvent(ShowMapViewEvent event) {
        showMapViewFragment();
        mBackFragmentTag = MAP_LIST_FRAGMENT_TAG;
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_map:
                showMapViewFragment();
                break;

            case R.id.nav_select_map:
                showMapListFragment();
                break;

            case R.id.nav_create:
                showMapCreateFragment();
                break;

            case R.id.nav_record:
                showRecordFragment();
                break;

            case R.id.nav_import:
                showMapImportFragment();
                break;

            case R.id.nav_track_stats:
                showTrackViewFragment();
                break;

            case R.id.nav_settings:
                showSettingsFragment();
                break;

            case R.id.nav_help:
                String url = getString(R.string.help_url);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);

            default:
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private Fragment createMapViewFragment(FragmentTransaction transaction) {
        Fragment mapFragment = new MapViewFragment();
        transaction.add(R.id.content_frame, mapFragment, MAP_FRAGMENT_TAG);
        return mapFragment;
    }

    private Fragment createMapSettingsFragment(FragmentTransaction transaction, int mapId) {
        Fragment mapSettingsFragment = MapSettingsFragment.newInstance(mapId);
        transaction.add(R.id.content_frame, mapSettingsFragment, MAP_SETTINGS_FRAGMENT_TAG);
        return mapSettingsFragment;
    }

    /**
     * Generic strategy to create a {@link Fragment}.
     */
    private <T extends Fragment> Fragment createFragment(FragmentTransaction transaction, String tag, Class<T> fragmentType) throws InstantiationException, IllegalAccessException {
        Fragment fragment = fragmentType.newInstance();
        transaction.add(R.id.content_frame, fragment, tag);
        return fragment;
    }

    private Fragment createWmtsViewFragment(FragmentTransaction transaction, MapSource mapSource) {
        Fragment wmtsViewFragment = GoogleMapWmtsViewFragment.newInstance(new MapSourceBundle(mapSource));
        transaction.add(R.id.content_frame, wmtsViewFragment, WMTS_VIEW_FRAGMENT_TAG);
        return wmtsViewFragment;
    }

    @Override
    public void onRequestManageTracks() {
        showTracksManageFragment();
    }

    @Override
    public void onRequestManageMarker(int mapId, @NonNull MarkerGson.Marker marker) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        Fragment fragment = MarkerManageFragment.Companion.newInstance(mapId, marker);

        hideAllFragments();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.content_frame, fragment, MARKER_MANAGE_FRAGMENT_TAG);
        transaction.show(fragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MARKER_MANAGE_FRAGMENT_TAG;
        transaction.commit();
    }

    public TracksManageFragment getTracksManageFragment() {
        Fragment fragment = fragmentManager.findFragmentByTag(TRACKS_MANAGE_FRAGMENT_TAG);
        return fragment != null ? (TracksManageFragment) fragment : null;
    }

    private void showMapViewFragment() {
        /* Don't show the fragment if no map has been selected yet */
        if (MapModel.INSTANCE.getCurrentMap() == null) {
            return;
        }

        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);

        if (mapFragment == null) {
            mapFragment = createMapViewFragment(transaction);
        }
        transaction.show(mapFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MAP_LIST_FRAGMENT_TAG;
        transaction.commit();
    }

    private void showTracksManageFragment() {
        try {
            showSingleUsageFragment(TRACKS_MANAGE_FRAGMENT_TAG, TracksManageFragment.class);
        } catch (IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "Error while creating " + TRACKS_MANAGE_FRAGMENT_TAG);
        }
    }

    private void showMarkerManageFragment() {
        try {
            showSingleUsageFragment(MARKER_MANAGE_FRAGMENT_TAG, MarkerManageFragment.class);
        } catch (IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "Error while creating " + MARKER_MANAGE_FRAGMENT_TAG);
        }
    }

    private <T extends Fragment> void showSingleUsageFragment(String tag, Class<T> fragmentType) throws IllegalAccessException, InstantiationException {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        T fragment = fragmentType.newInstance();

        hideAllFragments();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.content_frame, fragment, tag);
        transaction.show(fragment);

        /* Manually manage the back action*/
        mBackFragmentTag = tag;
        transaction.commit();
    }

    private void showMapListFragment() {
        showFragment(MAP_LIST_FRAGMENT_TAG, null, MapListFragment.class);
    }

    private void showMapSettingsFragment(int mapId) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_SETTINGS_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapSettingsFragment = fragmentManager.findFragmentByTag(MAP_SETTINGS_FRAGMENT_TAG);

        /* Show the map settings fragment if it exists */
        if (mapSettingsFragment == null) {
            mapSettingsFragment = createMapSettingsFragment(transaction, mapId);
        } else {
            /* If it already exists, set the Map */
            ((MapSettingsFragment) mapSettingsFragment).setMap(mapId);
        }
        transaction.show(mapSettingsFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MAP_LIST_FRAGMENT_TAG;
        transaction.commit();
    }

    /**
     * Generic way of showing a fragment.
     *
     * @param tag             the tag of the {@link Fragment} to show
     * @param backFragmentTag the tag of the {@link Fragment} the back press should lead to
     * @param fragmentType    the class of the {@link Fragment} to instantiate (if needed)
     */
    private <T extends Fragment> void showFragment(String tag, String backFragmentTag, Class<T> fragmentType) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, tag);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment;
        try {
            fragment = createFragment(transaction, tag, fragmentType);
            transaction.show(fragment);

            /* Manually manage the back action */
            mBackFragmentTag = backFragmentTag;
            transaction.commit();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void showMapCalibrationFragment() {
        showFragment(MAP_CALIBRATION_FRAGMENT_TAG, MAP_SETTINGS_FRAGMENT_TAG, MapCalibrationFragment.class);
    }

    private void showMapCreateFragment() {
        showFragment(MAP_CREATE_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, MapCreateFragment.class);

        if (!checkMapCreationPermission()) {
            requestMapCreationPermission();
        }
        warnIfNotInternet();
    }

    private void showIgnCredentialsFragment() {
        showFragment(IGN_CREDENTIALS_FRAGMENT_TAG, IGN_CREDENTIALS_FRAGMENT_TAG, IgnCredentialsFragment.class);
        warnIfNotInternet();
    }

    private void showWmtsViewFragment(MapSource mapSource) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, WMTS_VIEW_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment wmtsViewFragment = createWmtsViewFragment(transaction, mapSource);
        transaction.show(wmtsViewFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = WMTS_VIEW_FRAGMENT_TAG;
        transaction.commit();

        warnIfNotInternet();
    }

    private void showMapImportFragment() {
        showFragment(MAP_IMPORT_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, MapImportFragment.class);
    }

    private void showRecordFragment() {
        showFragment(RECORD_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, RecordFragment.class);
    }

    private void showTrackViewFragment() {
        showFragment(TRACK_VIEW_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, TrackViewFragment.class);
    }

    private void showSettingsFragment() {
        showFragment(SETTINGS_FRAGMENT, MAP_LIST_FRAGMENT_TAG, SettingsFragment.class);
    }

    /**
     * Hides all fragments except the one which tag is {@code fragmentTag}.
     * Oddly, with the exception of fragments extending {@link androidx.preference.PreferenceFragmentCompat},
     * or it would cause an exception (hard to explain).
     *
     * @param transaction The {@link FragmentTransaction} object
     * @param fragmentTag The tag of the fragment that should not be hidden
     */
    private void hideOtherFragments(FragmentTransaction transaction, String fragmentTag) {
        if (!FRAGMENT_TAGS.contains(fragmentTag)) {
            return;
        }
        for (String tag : FRAGMENT_TAGS) {
            if (tag.equals(fragmentTag)) continue;
            Fragment otherFragment = fragmentManager.findFragmentByTag(tag);
            if (otherFragment != null && !(otherFragment instanceof PreferenceFragmentCompat)) {
                transaction.hide(otherFragment);
            }
        }
    }

    /**
     * Only remove fragments that are NOT retained.
     */
    private void removeSingleUsageFragments() {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        for (String tag : FRAGMENT_TAGS) {
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null && !fragment.getRetainInstance()) {
                transaction.remove(fragment);
            }
        }

        transaction.commit();
    }

    /**
     * Hides all fragments which have a TAG.
     */
    private void hideAllFragments() {
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        for (String tag : FRAGMENT_TAGS) {
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                hideTransaction.hide(fragment);
            }
        }
        hideTransaction.commit();
    }

    private void showMessageInSnackbar(String message) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer == null) return;
        Snackbar snackbar = Snackbar.make(drawer, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void showWarningDialog(String message, String title, DialogInterface.OnDismissListener dismiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message).setTitle(title);
        if (dismiss != null) {
            builder.setOnDismissListener(dismiss);
        }
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * A map has been selected from the {@link MapListFragment}. <br>
     * Updates the current map reference and show the {@link MapViewFragment}.
     */
    @Override
    public void onMapSelectedFragmentInteraction(Map map) {
        showMapViewFragment();
    }

    @Override
    public void onMapSettingsFragmentInteraction(Map map) {
        /* The setting button of a map has been clicked */
        showMapSettingsFragment(map.getId());
    }

    @Override
    public void onGoToMapCreation() {
        showMapCreateFragment();
    }

    @Override
    public void onMapCalibrationRequest() {
        /* A map has been selected from the MapSettingsFragment to be calibrated. */
        showMapCalibrationFragment();
    }

    @Override
    public void onMapArchiveFragmentInteraction() {
        showMapListFragment();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        /* If Android >= 10 we don't need to restart the activity as we don't request write permission */
        if (android.os.Build.VERSION.SDK_INT >= Q) return;

        /* Else, we want to restart the activity */
        if (requestCode == REQUEST_MINIMAL) {
            if (grantResults.length >= 2) {
                /* Storage read perm is at index 1 */
                if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    warnNoStoragePerm();
                } else {
                    /* Restart the activity to ensure that every component can access local storage */
                    finish();
                    startActivity(getIntent());
                }
            }
        }
    }

    @Override
    public void showCurrentMap() {
        showMapViewFragment();
    }

    @Subscribe
    public void onMapSourceSelected(MapSourceSelectedEvent event) {
        MapSource mapSource = event.getMapSource();
        switch (mapSource) {
            case IGN:
                /* Check whether credentials are already set or not */
                IGNCredentials ignCredentials = MapSourceCredentials.INSTANCE.getIGNCredentials();
                if (ignCredentials == null) {
                    showIgnCredentialsFragment();
                } else {
                    showWmtsViewFragment(mapSource);
                }
                break;
            default:
                showWmtsViewFragment(mapSource);
                break;
        }
    }

    @Subscribe
    public void onMapSourceSettings(MapSourceSettingsEvent event) {
        MapSource mapSource = event.getMapSource();
        switch (mapSource) {
            case IGN:
                showIgnCredentialsFragment();
                break;
            case OPEN_STREET_MAP:
                // show OSM settings
                break;
            default:
                /* Unknown map source */
        }
    }

    private void setTrackStatsMenuVisibility(boolean visibility) {
        Menu menu = mNavigationView.getMenu();
        menu.findItem(R.id.nav_track_stats).setVisible(visibility);
    }

    @Subscribe
    public void onMapDownloadEvent(MapDownloadEvent event) {
        if (event.getStatus().equals(Status.STORAGE_ERROR)) {
            showWarningDialog(getString(R.string.service_download_bad_storage), getString(R.string.warning_title), null);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_BUNDLE_BACK, mBackFragmentTag);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mBackFragmentTag = savedInstanceState.getString(KEY_BUNDLE_BACK);
    }

    @NonNull
    @Override
    public LocationProvider getLocationProvider() {
        if (locationProviderFactory == null) {
            locationProviderFactory = new LocationProviderFactory(getApplicationContext());
        }
        return locationProviderFactory.getLocationProvider();
    }

    /**
     * A {@link Notification} is sent to the user showing the progression in percent. The
     * {@link NotificationManager} only process one notification at a time, which is handy since
     * it prevents the application from using too much cpu.
     */
    private void onZipProgressEvent(ZipProgressEvent event) {
        final String notificationChannelId = "trekadvisor_map_save";

        if (builder == null || notifyMgr == null) {
            /* Build the notification and issue it */
            builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_map_black_24dp)
                    .setContentTitle(getString(R.string.archive_dialog_title));

            try {
                notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            } catch (Exception e) {
                // notifyMgr will be null
            }

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                //This only needs to be run on Devices on Android O and above
                NotificationChannel mChannel = new NotificationChannel(notificationChannelId,
                        getText(R.string.archive_dialog_title), NotificationManager.IMPORTANCE_LOW);
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.YELLOW);
                if (notifyMgr != null) {
                    notifyMgr.createNotificationChannel(mChannel);
                }
                builder.setChannelId(notificationChannelId);
            }

            if (notifyMgr != null) {
                notifyMgr.notify(event.getMapId(), builder.build());
            }
        }

        builder.setContentText(String.format(getString(R.string.archive_notification_msg), event.getMapName()));
        builder.setProgress(100, event.getP(), false);
        if (notifyMgr != null) {
            notifyMgr.notify(event.getMapId(), builder.build());
        }
    }

    private void onZipFinishedEvent(ZipFinishedEvent event) {
        String archiveOkMsg = getString(R.string.archive_snackbar_finished);

        if (builder == null) return;
        /* When the loop is finished, updates the notification */
        builder.setContentText(archiveOkMsg)
                // Removes the progress bar
                .setProgress(0, 0, false);
        if (notifyMgr != null) {
            notifyMgr.notify(event.getMapId(), builder.build());
        }

        if (mNavigationView != null) {
            Snackbar snackbar = Snackbar.make(mNavigationView, archiveOkMsg, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }
}
