package amazin.bookstore.Controllers;

import amazin.bookstore.Book;
import amazin.bookstore.Recommendation;
import amazin.bookstore.Recommendation.Weighting;
import amazin.bookstore.User;
import amazin.bookstore.repositories.BookRepository;
import amazin.bookstore.repositories.RecommendationRepository;
import amazin.bookstore.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired
    private final RecommendationRepository recommendationRepository;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final BookRepository bookRepository;

    public RecommendationController(RecommendationRepository recommendationRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @GetMapping("")
    public String showRecommendations(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/user/login";
        }
        User user = userRepository.findById((Long) session.getAttribute("userId")).orElseThrow();
        List<Recommendation> recommendations = recommendationRepository.findByUserOrderByWeightDesc(user);
        ArrayList<Book> recommendedBooks = new ArrayList<>();
        for (Recommendation r : recommendations) {
            recommendedBooks.add(r.getBook());
        }
        model.addAttribute("recommendations", recommendedBooks);
        return "recommendations";
    }

    @GetMapping("/refresh")
    public String refreshRecommendations(HttpSession session, Model model) {
        User user = userRepository.findById((Long) session.getAttribute("userId")).orElse(null);
        if (user == null) return "redirect:/user/login";

        List<Book> history = user.getPurchasedBooks();
        List<Book> catalogue = (List<Book>) bookRepository.findAll(Sort.unsorted());

        for (Book b : catalogue) {
            if (recommendationRepository.findByUserAndBook(user, b) == null) {
                for (Book b1 : history) {
                    if (!history.contains(b)) {
                        if (b1.getAuthor().equals(b.getAuthor())) {
                            recommendationRepository.save(new Recommendation(user, b, Weighting.SAME_AUTHOR.value()));
                            break;
                        } else if (b1.getDescription().equals(b.getDescription())) {
                            recommendationRepository.save(new Recommendation(user, b, Weighting.SAME_GENRE.value()));
                            break;
                        }
                    }
                }
            }
        }

        return "redirect:/recommendations";
    }
}