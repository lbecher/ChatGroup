package br.unioeste.ChatGroup;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServidorController {
    @GetMapping("/servidor")
    public String run() {
        
    }
}
