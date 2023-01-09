package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.databinding.DialogRenameFileFolderBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentFilesBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class TorrentFilesFragment() : Fragment(R.layout.fragment_torrent_files) {
    private val binding by viewBinding(FragmentTorrentFilesBinding::bind)
    private val activityBinding by viewBinding(ActivityTorrentBinding::bind, viewProvider = { requireActivity().view })

    private val viewModel: TorrentFilesViewModel by viewModels()

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverConfig: ServerConfig, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverConfig" to serverConfig,
            "torrentHash" to torrentHash
        )
    }

    private lateinit var onPageChange: ViewPager2.OnPageChangeCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var actionMode: ActionMode? = null
        val adapter = TorrentFilesAdapter().apply {
            onClick { file ->
                if (file.isFolder) {
                    viewModel.goToFolder(file.name)
                }
            }
            onSelectionModeStart {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_files_selection_menu, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.menu_priority_do_not_download -> {
                                viewModel.setFilePriority(
                                    serverConfig = serverConfig,
                                    hash = torrentHash,
                                    files = getSelectedFiles(),
                                    priority = TorrentFilePriority.DO_NOT_DOWNLOAD
                                )
                                finishSelection()
                                actionMode?.finish()
                            }
                            R.id.menu_priority_normal -> {
                                viewModel.setFilePriority(
                                    serverConfig = serverConfig,
                                    hash = torrentHash,
                                    files = getSelectedFiles(),
                                    priority = TorrentFilePriority.NORMAL
                                )
                                finishSelection()
                                actionMode?.finish()
                            }
                            R.id.menu_priority_high -> {
                                viewModel.setFilePriority(
                                    serverConfig = serverConfig,
                                    hash = torrentHash,
                                    files = getSelectedFiles(),
                                    priority = TorrentFilePriority.HIGH
                                )
                                finishSelection()
                                actionMode?.finish()
                            }
                            R.id.menu_priority_maximum -> {
                                viewModel.setFilePriority(
                                    serverConfig = serverConfig,
                                    hash = torrentHash,
                                    files = getSelectedFiles(),
                                    priority = TorrentFilePriority.MAXIMUM
                                )
                                finishSelection()
                                actionMode?.finish()
                            }
                            R.id.menu_rename -> {
                                val key = selectedItems.first()
                                val fileName = key.drop(1)
                                val isFile = key.startsWith("1")

                                val nodeStack = viewModel.nodeStack.value
                                val root = if (nodeStack.isNotEmpty()) {
                                    val folderList = mutableListOf<String>()
                                    for (folder in nodeStack.descendingIterator()) {
                                        folderList.add(folder)
                                    }
                                    folderList.joinToString("/") + "/"
                                } else {
                                    ""
                                }

                                showRenameFileFolderDialog(
                                    name = "$root$fileName",
                                    isFile = isFile,
                                    onSuccess = {
                                        finishSelection()
                                        actionMode?.finish()
                                    }
                                )
                            }
                            R.id.menu_select_all -> {
                                selectAll()
                            }
                            R.id.menu_select_inverse -> {
                                selectInverse()
                            }
                            else -> return false
                        }
                        return true
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        finishSelection()
                        actionMode = null
                    }
                })
            }
            onSelectionModeEnd {
                actionMode?.finish()
            }
            onUpdateSelection {
                val itemCount = selectedItemCount
                if (itemCount != 0) {
                    actionMode?.title = resources.getQuantityString(
                        R.plurals.torrent_files_menu_files_selected,
                        itemCount,
                        itemCount
                    )
                    actionMode?.menu?.findItem(R.id.menu_rename)?.isEnabled = itemCount == 1
                }
            }
        }
        val backButtonAdapter = TorrentFilesBackButtonAdapter(
            onClick = {
                viewModel.goBack()
            }
        )
        binding.recyclerFiles.adapter = ConcatAdapter(backButtonAdapter, adapter)

        onPageChange = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                actionMode?.finish()
            }
        }
        activityBinding.viewPager.registerOnPageChangeCallback(onPageChange)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshFiles(serverConfig, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadFiles(serverConfig, torrentHash)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        combine(viewModel.nodeStack, viewModel.torrentFiles) { nodeStack, torrentFiles ->
            nodeStack to torrentFiles
        }.launchAndCollectLatestIn(viewLifecycleOwner) { (nodeStack, torrentFiles) ->
            if (torrentFiles != null) {
                val fileList = torrentFiles.findChildNode(nodeStack)?.children?.sortedWith(
                    compareBy({ it.isFile }, { it.name.lowercase() })
                )
                if (fileList != null) {
                    adapter.submitList(fileList) {
                        if (nodeStack.isNotEmpty()) {
                            backButtonAdapter.currentDirectory = nodeStack.toList().reversed().joinToString("/")
                        } else {
                            backButtonAdapter.currentDirectory = null
                        }
                    }
                } else {
                    viewModel.goToRoot()
                }
            }
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner, Lifecycle.State.RESUMED) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive) {
                        viewModel.loadFiles(serverConfig, torrentHash)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentFilesViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentFilesViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found)
                }
                TorrentFilesViewModel.Event.PathIsInvalidOrInUse -> {
                    showSnackbar(R.string.torrent_files_error_path_is_invalid_or_in_use)
                }
                TorrentFilesViewModel.Event.FilePriorityUpdated -> {
                    showSnackbar(R.string.torrent_files_priority_update_success)
                    viewModel.loadFiles(serverConfig, torrentHash)
                }
                TorrentFilesViewModel.Event.FileRenamed -> {
                    showSnackbar(R.string.torrent_files_file_renamed_success)
                    viewModel.loadFiles(serverConfig, torrentHash)
                }
                TorrentFilesViewModel.Event.FolderRenamed -> {
                    showSnackbar(R.string.torrent_files_folder_renamed_success)
                    viewModel.loadFiles(serverConfig, torrentHash)
                }
            }
        }
    }

    private fun showRenameFileFolderDialog(name: String, isFile: Boolean, onSuccess: () -> Unit) {
        showDialog(DialogRenameFileFolderBinding::inflate) { binding ->
            binding.inputLayoutName.setTextWithoutAnimation(name)
            binding.inputLayoutName.setHint(
                if (isFile) {
                    R.string.torrent_files_rename_file_hint
                } else {
                    R.string.torrent_files_rename_folder_hint
                }
            )

            setTitle(
                if (isFile) {
                    R.string.torrent_files_rename_file_dialog_title
                } else {
                    R.string.torrent_files_rename_folder_dialog_title
                }
            )
            setPositiveButton { _, _ ->
                val newName = binding.editName.text.toString()

                if (isFile) {
                    viewModel.renameFile(serverConfig, torrentHash, name, newName)
                } else {
                    viewModel.renameFolder(serverConfig, torrentHash, name, newName)
                }
                onSuccess()
            }
            setNegativeButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activityBinding.viewPager.unregisterOnPageChangeCallback(onPageChange)
    }
}
