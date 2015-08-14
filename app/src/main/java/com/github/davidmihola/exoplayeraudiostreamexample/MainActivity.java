package com.github.davidmihola.exoplayeraudiostreamexample;

import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 160;
    private static final String USER_AGENT = "Android";

    private static final SparseArray<String> EXO_PLAYER_STATES = new SparseArray<String>(5);

    {
        EXO_PLAYER_STATES.put(ExoPlayer.STATE_BUFFERING, "STATE_BUFFERING");
        EXO_PLAYER_STATES.put(ExoPlayer.STATE_ENDED, "STATE_ENDED");
        EXO_PLAYER_STATES.put(ExoPlayer.STATE_IDLE, "STATE_IDLE");
        EXO_PLAYER_STATES.put(ExoPlayer.STATE_PREPARING, "STATE_PREPARING");
        EXO_PLAYER_STATES.put(ExoPlayer.STATE_READY, "STATE_READY");
    }

    private ExoPlayer.Listener mExoPlayerListener = new ExoPlayer.Listener() {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Timber.d("onPlayerStateChanged: %b --- %s", playWhenReady, EXO_PLAYER_STATES.get(playbackState));
        }

        @Override
        public void onPlayWhenReadyCommitted() {
            Timber.d("onPlayWhenReadyCommited");
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            Timber.e(e, "onPlayerError");
        }
    };

    private MediaCodecAudioTrackRenderer.EventListener mMediaCodecAudioTrackRendererEventListener = new MediaCodecAudioTrackRenderer.EventListener() {
        @Override
        public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
            Timber.e(e, "onAudioTrackInitializationError");
        }

        @Override
        public void onAudioTrackWriteError(AudioTrack.WriteException e) {
            Timber.e(e, "onAudioTrackWriteError");
        }

        @Override
        public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
            Timber.e(e, "onDecoderInitializationError");
        }

        @Override
        public void onCryptoError(MediaCodec.CryptoException e) {
            Timber.e(e, "onCryptoError");
        }

        @Override
        public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {
            Timber.d("onDecoderInitialized %s, %d, %d", decoderName, elapsedRealtimeMs, initializationDurationMs);
        }
    };

    private Handler mHandler;
    private ExoPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final ViewGroup messages = (ViewGroup) findViewById(R.id.messages);

        Timber.plant(new Timber.DebugTree());
        Timber.plant(new Timber.Tree() {
            @Override
            protected void log(int priority, String tag, String message, Throwable t) {
                final TextView textView = new TextView(MainActivity.this);
                textView.setText(message);
                messages.addView(textView);
            }
        });

        mHandler = new Handler();
        mPlayer = ExoPlayer.Factory.newInstance(1);
        mPlayer.addListener(mExoPlayerListener);

        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mHandler, null);
        DataSource dataSource = new DefaultUriDataSource(this, bandwidthMeter, USER_AGENT);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(Uri.parse("http://live.antenne.at/as"), dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                null, true, mHandler, mMediaCodecAudioTrackRendererEventListener);
        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[1];
        renderers[0] = audioRenderer;
        mPlayer.prepare(renderers);
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
