package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityAddTorrentBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelable
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar

@AndroidEntryPoint
class AddTorrentActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_CONFIG = "dev.bartuzen.qbitcontroller.SERVER_CONFIG"

        const val IS_ADDED = "dev.bartuzen.qbitcontroller.IS_ADDED"
    }

    private lateinit var binding: ActivityAddTorrentBinding

    private val viewModel: AddTorrentViewModel by viewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serverConfig = intent.getParcelable<ServerConfig>(Extras.SERVER_CONFIG)
        if (serverConfig == null) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.editTorrentLink.setOnTouchListener { _, _ ->
            binding.scrollView.requestDisallowInterceptTouchEvent(true)
            false
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.add_torrent_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
                R.id.menu_add -> {
                    val links = binding.editTorrentLink.text.toString()
                    if (links.isNotBlank()) {
                        viewModel.createTorrent(
                            serverConfig = serverConfig,
                            links = links.split("\n"),
                            isPaused = !binding.checkBoxStartTorrent.isChecked,
                            skipHashChecking = binding.checkBoxSkipChecking.isChecked,
                            isAutoTorrentManagementEnabled = binding.checkBoxAutoTmm.isChecked,
                            isSequentialDownloadEnabled = binding.checkBoxSequentialDownload.isChecked,
                            isFirstLastPiecePrioritized = binding.checkBoxPrioritizeFirstLastPiece.isChecked
                        )
                    } else {
                        showSnackbar(R.string.torrent_add_link_cannot_be_empty)
                    }
                    true
                }
                else -> false
            }
        })

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is AddTorrentViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(this@AddTorrentActivity, event.error))
                }
                AddTorrentViewModel.Event.TorrentCreated -> {
                    val intent = Intent().apply {
                        putExtra(Extras.IS_ADDED, true)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        }
    }
}