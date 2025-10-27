// Пакет, в котором находится фрагмент — отображение списка подразделений (отделов, цехов и т.п.)
package ru.contlog.mobile.helper.fragments

// Импорты стандартных и сторонних классов
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.FragmentDivisionsListBinding
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.rvadapters.DivisionsRVAdapter
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory


/*
 * Фрагмент для отображения:
 * - информации о текущем пользователе (ФИО, должность, аватар),
 * - списка подразделений (отделов),
 * - ошибок (если они есть),
 * - кнопки выхода из аккаунта.
 */
class DivisionsListFragment : Fragment() {
    // ViewBinding для удобной работы с элементами интерфейса
    private lateinit var binding: FragmentDivisionsListBinding

    // ViewModel, управляющий бизнес-логикой и данными.
    // Создаётся с помощью фабрики, в которую передаётся репозиторий настроек.
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this.requireContext()))
    }

    /**
     * Создаёт и возвращает корневой View фрагмента с помощью ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDivisionsListBinding.inflate(inflater)
        return binding.root
    }

    /**
     * Настраивает UI и подписывается на изменения данных из ViewModel.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // === Наблюдение за ошибками ===
        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            if (errors.isEmpty()) {
                // Если ошибок нет — скрываем блок с ошибками
                binding.errorsCard.visibility = View.GONE
                binding.errorsText.text = ""
                return@observe
            }

            // Формируем нумерованный список ошибок для отображения
            val errorMessage = errors.mapIndexed { i, e ->
                "${i+1}. ${e.message ?: e}"
            }.joinToString("\n")
            binding.errorsText.text = errorMessage
            binding.errorsCard.visibility = View.VISIBLE

            // При наличии ошибок скрываем остальной контент
            binding.userInfoCard.visibility = View.GONE
            binding.divisionsList.visibility = View.GONE
            binding.divisionsListEmptyInfo.visibility = View.GONE
        }

        // === Наблюдение за данными пользователя ===
        viewModel.userData.observe(viewLifecycleOwner) { userData ->
            if (userData == null) {
                binding.userInfoCard.visibility = View.GONE
                return@observe
            }

            // Заполняем поля: ФИО и должность
            binding.fullName.text = userData.name
            binding.position.text = userData.position

            // Асинхронно декодируем фото из Base64 и устанавливаем в ImageView
            lifecycleScope.launch(Dispatchers.Default) {
                val bytes = Base64.decode(userData.photo, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                // Обновляем UI в основном потоке
                launch(Dispatchers.Main) {
                    binding.userAvatar.setImageBitmap(decodedBitmap)
                }
            }

            // Показываем карточку пользователя и скрываем индикатор обновления
            binding.userInfoCard.visibility = View.VISIBLE
            binding.refresh.isRefreshing = false
        }

        // === Настройка RecyclerView для списка подразделений ===
        val adapter = DivisionsRVAdapter { division ->
            // При клике на элемент — переходим к следующему фрагменту,
            // передавая выбранное подразделение через Bundle
            val bundle = bundleOf("division" to division)
            findNavController().navigate(R.id.action_workSitesFragment_to_productInfoFragment, bundle)
        }
        // Подписываемся на изменения списка подразделений
        viewModel.division.observe(viewLifecycleOwner) { divisions ->
            adapter.setData(divisions)          // Обновляем данные в адаптере
            // Показываем/скрываем пустое состояние в зависимости от наличия данных
            binding.divisionsListEmptyInfo.visibility = if (divisions.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.divisionsList.visibility = if (divisions.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Настраиваем RecyclerView: вертикальный LinearLayoutManager и адаптер
        binding.divisionsList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.divisionsList.adapter = adapter

        // === Обработка "потянуть для обновления" ===
        binding.refresh.setOnRefreshListener {
            getData()       // Запрашиваем свежие данные
        }

        // === Обработка нажатия на кнопку "Выйти" ===
        binding.logout.setOnClickListener {
            // Переход на экран логина
            findNavController().navigate(R.id.action_workSitesFragment_to_loginFragment)
            // Очистка сессии в ViewModel
            viewModel.logout()
        }

        // Запрашиваем данные сразу после отображения фрагмента
        binding.root.post {
            getData()
        }
    }

    /**
     * Запрашивает данные пользователя и список подразделений параллельно.
     */
    private fun getData() {
        viewModel.clearErrors()                     // Очищаем предыдущие ошибки
        lifecycleScope.launch(Dispatchers.IO) {
            // Показываем индикатор обновления
            launch(Dispatchers.Main) {
                binding.refresh.isRefreshing = true
            }

            // Выполняем два запроса одновременно (параллельно)
            awaitAll(
                async {
                    viewModel.fetchUserData()
                },
                async {
                    viewModel.fetchDivisions()
                }
            )

            // Скрываем индикатор после завершения
            launch(Dispatchers.Main) {
                binding.refresh.isRefreshing = false
            }
        }
    }
}