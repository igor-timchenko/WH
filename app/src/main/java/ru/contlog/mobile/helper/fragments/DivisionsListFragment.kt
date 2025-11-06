package ru.contlog.mobile.helper.fragments

// Импорты стандартных и сторонних библиотек
import android.graphics.BitmapFactory  // Для работы с изображениями (возможно, используется в другом месте)
import android.os.Bundle                // Класс для передачи данных между компонентами
import android.util.Base64              // Для декодирования Base64 (возможно, для изображений или токенов)
import android.util.Log                 // Для логирования
import android.view.LayoutInflater      // Для создания UI из XML
import android.view.View                // Базовый класс представления
import android.view.ViewGroup           // Контейнер для View
import androidx.core.os.bundleOf        // Удобный способ создания Bundle
import androidx.fragment.app.Fragment   // Базовый класс фрагмента
import androidx.fragment.app.activityViewModels // Для использования ViewModel, привязанной к активности
import androidx.fragment.app.viewModels // Альтернатива для ViewModel, привязанной к фрагменту (здесь не используется)
import androidx.lifecycle.lifecycleScope // Корутины, привязанные к жизненному циклу
import androidx.navigation.fragment.findNavController // Для навигации между фрагментами
import androidx.recyclerview.widget.LinearLayoutManager // LayoutManager для RecyclerView
import kotlinx.coroutines.Dispatchers    // Диспетчеры корутин (IO, Main и т.д.)
import kotlinx.coroutines.async         // Для параллельного выполнения задач
import kotlinx.coroutines.awaitAll      // Ожидание завершения всех async-задач
import kotlinx.coroutines.launch        // Запуск корутины
import ru.contlog.mobile.helper.R       // Ресурсы приложения
import ru.contlog.mobile.helper.databinding.FragmentDivisionsListBinding // ViewBinding для этого фрагмента
import ru.contlog.mobile.helper.repo.AppPreferencesRepository // Репозиторий настроек (здесь не используется напрямую)
import ru.contlog.mobile.helper.rvadapters.DivisionsRVAdapter // Адаптер для RecyclerView
import ru.contlog.mobile.helper.vm.AppViewModel // Общий ViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory // Фабрика ViewModel (здесь не используется)

// Класс фрагмента списка подразделений
class DivisionsListFragment : Fragment() {
    // ViewBinding для доступа к UI-элементам без findViewById
    private lateinit var binding: FragmentDivisionsListBinding

    // Используем activity-scoped ViewModel, чтобы данные сохранялись при навигации между фрагментами
    private val viewModel: AppViewModel by activityViewModels()

    // Создаём корневое представление из layout-файла с помощью ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDivisionsListBinding.inflate(inflater)
        return binding.root
    }

    // Вызывается после создания View — здесь настраиваем UI и подписываемся на данные
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Логируем текущее состояние userData (для отладки)
        Log.i("asdf1", "onViewCreated: ${viewModel.userData.value}")

        // Подписываемся на ошибки из ViewModel
        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            // Если ошибок нет — скрываем карточку ошибок
            if (errors.isEmpty()) {
                binding.errorsCard.visibility = View.GONE
                binding.errorsText.text = ""
                return@observe
            }

            // Форматируем список ошибок в читаемый вид (с нумерацией)
            val errorMessage = errors.mapIndexed { i, e ->
                "${i+1}. ${e.message ?: e}"
            }.joinToString("\n")
            // Отображаем ошибки
            binding.errorsText.text = errorMessage
            binding.errorsCard.visibility = View.VISIBLE

            // Скрываем список подразделений и сообщение "пусто", если есть ошибки
            binding.divisionsList.visibility = View.GONE
            binding.divisionsListEmptyInfo.visibility = View.GONE
        }

        // Подписка на ошибки — показываем только ошибку, скрываем профиль
        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            if (errors.isNotEmpty()) {
                binding.errorsText.text = "Ошибка соединения, проверьте подключение"
                binding.errorsCard.visibility = View.VISIBLE
                binding.divisionsListEmptyInfo.visibility = View.GONE
            } else {
                binding.errorsCard.visibility = View.GONE
            }
        }
        // Создаём адаптер RecyclerView с лямбдой-обработчиком клика по элементу
        val adapter = DivisionsRVAdapter { division ->
            // При клике создаём Bundle с выбранным подразделением
            val bundle = bundleOf("division" to division)
            // Переходим к фрагменту информации о товаре
            findNavController().navigate(R.id.action_workSitesFragment_to_productInfoFragment, bundle)
        }

        // Подписываемся на изменения списка подразделений
        viewModel.division.observe(viewLifecycleOwner) { divisions ->
            // Обновляем данные в адаптере
            adapter.setData(divisions)

            // Показываем/скрываем сообщение "нет данных", в зависимости от количества элементов
            binding.divisionsListEmptyInfo.visibility = if (divisions.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            // Показываем/скрываем сам список
            binding.divisionsList.visibility = if (divisions.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Настраиваем RecyclerView: вертикальный список
        binding.divisionsList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        // Устанавливаем адаптер
        binding.divisionsList.adapter = adapter

        // Обработчик обновления (свайп вниз)
        binding.refresh.setOnRefreshListener {
            getData() // Загружаем данные заново
        }

        // Запускаем загрузку данных после полной инициализации UI
        binding.root.post {
            getData()
        }
    }

    // Метод для загрузки данных с сервера
    private fun getData() {
        // Очищаем предыдущие ошибки
        viewModel.clearErrors()
        // Запускаем корутину в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            // Показываем индикатор обновления на главном потоке
            launch(Dispatchers.Main) {
                binding.refresh.isRefreshing = true
            }
            // Выполняем две задачи параллельно: загрузка данных пользователя и списка подразделений
            awaitAll(
                async {
                    viewModel.fetchUserData()
                },
                async {
                    viewModel.fetchDivisions()
                }
            )
            // Скрываем индикатор обновления
            launch(Dispatchers.Main) {
                binding.refresh.isRefreshing = false
            }
        }
    }
}