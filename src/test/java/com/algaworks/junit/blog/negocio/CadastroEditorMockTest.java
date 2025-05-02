package com.algaworks.junit.blog.negocio;

import com.algaworks.junit.blog.armazenamento.ArmazenamentoEditor;
import com.algaworks.junit.blog.exception.EditorNaoEncontradoException;
import com.algaworks.junit.blog.exception.RegraNegocioException;
import com.algaworks.junit.blog.modelo.Editor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class CadastroEditorMockTest {


    @Captor
    ArgumentCaptor<Mensagem> mensagemArgumentCaptor;

    @Mock
    ArmazenamentoEditor armazenamentoEditor;
    @Mock
    GerenciadorEnvioEmail gerenciadorEnvioEmail;
    @InjectMocks
    CadastroEditor cadastroEditor;

    @Nested
    class CadastroComEditorValido {
        @Spy // Com o @spy ele sempre é instanciado em cada teste.
        Editor editor = new Editor(null, "Alex", "alex@alex.com", BigDecimal.TEN, true);


        @BeforeEach
        void init() {
            Mockito.when(armazenamentoEditor.salvar(Mockito.any(Editor.class)))
                    .thenAnswer(invocationOnMock -> {
                        Editor editorPassado = invocationOnMock.getArgument(0, Editor.class);
                        editorPassado.setId(1L);
                        return editorPassado;
                    });
        }

        @Test
        void Dado_um_editor_valido_quando_criar_entao_deve_retornar_um_id_de_cadastro() {
            Editor editorSalvo = cadastroEditor.criar(editor);

            Assertions.assertEquals(1L, editorSalvo.getId());
        }

        @Test
        void Dado_um_editor_valido_Quando_criar_Entao_deve_chamar_metodo_salvar_do_armazenamento() {
            cadastroEditor.criar(editor);

            //Verifica se o metodo salvar foi passado no teste com o parametro do editor.
            Mockito.verify(armazenamentoEditor, Mockito.times(1))
                    .salvar(Mockito.eq(editor));
        }

        @Test
        void Dado_um_editor_valido_Quando_criar_e_lancar_exception_ao_salvar_Entao_nao_deve_enviar_email() {
            Mockito.when(armazenamentoEditor.salvar(editor))
                    .thenThrow(new RuntimeException());

            Assertions.assertAll("Não deve enviar e-mail, quando lançar exception do armazenamento.",
                    () -> Assertions.assertThrows(RuntimeException.class, () -> cadastroEditor.criar(editor)),
                    () -> Mockito.verify(gerenciadorEnvioEmail, Mockito.never()).enviarEmail(Mockito.any()));
        }

        @Test
        void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_enviar_email_com_destino_ao_editor() {
            // ArgumentCaptor<Mensagem> mensagemArgumentCaptor = ArgumentCaptor.forClass(Mensagem.class);

            Editor editorSalvo = cadastroEditor.criar(editor);

            //pega o argumento passado no metodo criar
            Mockito.verify(gerenciadorEnvioEmail).enviarEmail(mensagemArgumentCaptor.capture());

            Mensagem mensagem = mensagemArgumentCaptor.getValue();

            Assertions.assertEquals(editorSalvo.getEmail(), mensagem.getDestinatario());
        }

        @Test
        void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_verificar_o_email() {
            cadastroEditor.criar(editor);
            Mockito.verify(editor, Mockito.atLeast(1)).getEmail();
        }

        @Test
        void Dado_um_editor_com_email_existente_Quando_cadastrar_Entao_deve_lancar_exception() {
            Mockito.when(armazenamentoEditor.encontrarPorEmail("alex@alex.com"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(editor));

            Editor editorComEmailExistente = new Editor(null, "Alex", "alex@alex.com", BigDecimal.TEN, true);
            cadastroEditor.criar(editor);
            Assertions.assertThrows(RegraNegocioException.class, () -> cadastroEditor.criar(editorComEmailExistente));
        }

        @Test
        void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_enviar_email_apos_salvar() {
            cadastroEditor.criar(editor);

            InOrder inOrder = Mockito.inOrder(armazenamentoEditor, gerenciadorEnvioEmail);

            inOrder.verify(armazenamentoEditor, Mockito.times(1)).salvar(editor);
            inOrder.verify(gerenciadorEnvioEmail, Mockito.times(1)).enviarEmail(Mockito.any(Mensagem.class));

        }
    }

    @Nested
    class CadastroComEditorNull {
        @Spy // Com o @spy ele sempre é instanciado em cada teste.
        Editor editor = new Editor(1L, "Alex", "alex@alex.com", BigDecimal.TEN, true);

        @Test
        void Dado_um_editor_null_Quando_cadastrar_Entao_deve_lanacar_exception() {
            Assertions.assertThrows(NullPointerException.class, () -> cadastroEditor.criar(null));
            Mockito.verify(armazenamentoEditor, Mockito.never()).salvar(editor);
            Mockito.verify(gerenciadorEnvioEmail, Mockito.never())
                    .enviarEmail(Mockito.any(Mensagem.class));
        }
    }

    @Nested
    class EdicaoComEditorValido {
        @Spy // Com o @spy ele sempre é instanciado em cada teste.
        Editor editor = new Editor(1L, "Alex", "alex@alex.com", BigDecimal.TEN, true);

        @BeforeEach
        void init() {
            Mockito.when(armazenamentoEditor.salvar(editor))
                    .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, Editor.class));
            Mockito.when(armazenamentoEditor.encontrarPorId(1L)).thenReturn(Optional.of(editor));
        }

        @Test
        void Dado_um_editor_valido_Quando_editar_Entao_deve_alterar_editor_salvo() {
            Editor editorAtualizado = new Editor(1L, "Alex", "alex@alex.com", BigDecimal.TEN, false);
            cadastroEditor.editar(editorAtualizado);

            Mockito.verify(editor, Mockito.times(1)).atualizarComDados(editorAtualizado);

            InOrder inOrder = Mockito.inOrder(editor, armazenamentoEditor);

            inOrder.verify(editor).atualizarComDados(editorAtualizado);
            inOrder.verify(armazenamentoEditor).salvar(editor);

        }
    }

    @Nested
    class EdicaoComEditorInexistente {
        Editor editor = new Editor(99L, "Alex", "alex@alex.com", BigDecimal.TEN, false);

        @BeforeEach
        void init() {
            Mockito.when(armazenamentoEditor.encontrarPorId(99L)).thenReturn(Optional.empty());
        }

        @Test
        void Dado_um_editor_que_nao_exista_Quando_editar_Entao_deve_lancar_exception() {
            Assertions.assertThrows(EditorNaoEncontradoException.class, () -> cadastroEditor.editar(editor));
            Mockito.verify(armazenamentoEditor, Mockito.never()).salvar(Mockito.any(Editor.class));
        }
    }

}
