package ru.contlog.mobile.helper.fragments

// Импорты стандартных и сторонних библиотек
import android.graphics.BitmapFactory  // Для декодирования изображений из массива байтов
import android.os.Bundle                // Класс для передачи данных между компонентами Android
import android.util.Base64              // Для декодирования строки Base64 (фото пользователя)
import android.util.Log                 // Для логирования (в этом коде не используется, но импортирован)
import android.view.LayoutInflater      // Для создания UI из XML-разметки
import android.view.View                // Базовый класс представления
import android.view.ViewGroup           // Контейнер для View
import androidx.core.os.bundleOf        // Утилита для удобного создания Bundle (здесь не используется напрямую)
import androidx.fragment.app.Fragment   // Базовый класс фрагмента
import androidx.fragment.app.activityViewModels // Делегат для получения ViewModel, привязанной к активности
import androidx.fragment.app.viewModels // Делегат для ViewModel, привязанной к фрагменту (здесь не используется)
import androidx.lifecycle.lifecycleScope // Область корутин, привязанная к жизненному циклу фрагмента/активности
import androidx.navigation.fragment.findNavController // Утилита для навигации между фрагментами
import androidx.recyclerview.widget.LinearLayoutManager // LayoutManager для RecyclerView (здесь не используется, но импортирован)
import kotlinx.coroutines.Dispatchers    // Диспетчеры корутин (Main, IO, Default и др.)
import kotlinx.coroutines.async         // Для параллельного выполнения асинхронных задач
import kotlinx.coroutines.awaitAll      // Ожидание завершения всех async-задач
import kotlinx.coroutines.launch        // Запуск корутины
import ru.contlog.mobile.helper.R       // Сгенерированный класс ресурсов приложения
import ru.contlog.mobile.helper.databinding.FragmentDivisionsListBinding // ViewBinding другого фрагмента (импортирован по ошибке, но не используется)
import ru.contlog.mobile.helper.databinding.FragmentProfileBinding // ViewBinding для этого фрагмента
import ru.contlog.mobile.helper.repo.AppPreferencesRepository // Репозиторий настроек (здесь не используется напрямую)
import ru.contlog.mobile.helper.rvadapters.DivisionsRVAdapter // Адаптер для RecyclerView (не используется в этом фрагменте)
import ru.contlog.mobile.helper.vm.AppViewModel // Основной ViewModel с данными пользователя и авторизации
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory // Фабрика ViewModel (здесь не используется)
import kotlin.getValue                  // Не используется напрямую, но может быть для делегатов

// Фрагмент профиля пользователя
class ProfileFragment : Fragment() {
    // ViewBinding для доступа к UI-элементам без findViewById
    private lateinit var binding: FragmentProfileBinding

    // Используем activity-scoped ViewModel, чтобы данные (например, авторизация) сохранялись при навигации
    private val viewModel: AppViewModel by activityViewModels()

    // Создаём корневое представление из layout-файла с помощью ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater)
        return binding.root
    }

    // Вызывается после создания View — здесь настраиваем UI и подписываемся на данные
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Подписываемся на ошибки — ОТДЕЛЬНО от userData
        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            if (errors.isNotEmpty()) {
                // Форматируем ошибки
                val errorMessage = errors.mapIndexed { i, e ->
                    "${i + 1}. ${e.message ?: e}"
                }.joinToString("\n")
                binding.errorsText.text = errorMessage
                binding.errorsCard.visibility = View.VISIBLE
                // 🔴 Скрываем профиль, даже если данные есть
                binding.userInfoCard.visibility = View.GONE
            } else {
                binding.errorsCard.visibility = View.GONE
                // Не показываем профиль здесь — это делает подписка на userData
            }
        }

        // Подписываемся на данные пользователя
        viewModel.userData.observe(viewLifecycleOwner) { userData ->
            // Если есть ошибки — не показываем профиль (уже скрыт выше)
            if (viewModel.errors.value?.isNotEmpty() == true) {
                return@observe
            }

            if (userData == null) {
                binding.userInfoCard.visibility = View.GONE
            } else {
                binding.fullName.text = userData.name
                binding.position.text = userData.position

                lifecycleScope.launch(Dispatchers.Default) {
                    val bytes = Base64.decode(userData.photo, Base64.DEFAULT)
                    val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    launch(Dispatchers.Main) {
                        binding.userAvatar.setImageBitmap(decodedBitmap)
                    }
                }
                binding.userInfoCard.visibility = View.VISIBLE
            }
        }

        // Обработчики
        binding.refresh.setOnRefreshListener {
            getData()
        }

        binding.logout.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            viewModel.logout()
        }
    }

    // Метод для загрузки данных пользователя и подразделений
    private fun getData() {
        // Очищаем предыдущие ошибки (если были)
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
            // Скрываем индикатор обновления на главном потоке
            launch(Dispatchers.Main) {
                binding.refresh.isRefreshing = false
            }
        }
    }
}